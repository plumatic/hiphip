(ns hiphip.test-utils
  "Shared utilities for defining joint tests/benchmarks for array operations."
  (:require
   [clojure.pprint :as pprint]
   [clojure.test :as test]
   [criterium.core :as criterium]
   [hiphip.impl.core :as impl]))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generic helpers

(defmacro result-or-ex [form]
  `(try ~form (catch Exception e# {:exception e#})))

(defn- array-class? [x]
  (or (#{'objects 'doubles 'floats 'longs 'ints 'shorts 'chars 'bytes 'booleans} x)
      (and (string? x) (.startsWith ^String x "["))))

(defn clone-args [arg-syms]
  (->> (for [a arg-syms] [a (if (array-class? (:tag (meta a)))
                              `(aclone ~a)
                              a)])
       (apply concat)
       vec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generating tests for equivalence of benchmark forms

(defmacro expr-results [arg-syms expr]
  `(let ~(clone-args arg-syms)
     {:expr (quote ~expr)
      :args ~arg-syms
      :result (result-or-ex ~expr)}))

(defn =-able
  "Convert result into something that can be tested for value equality"
  [result]
  (if (or (nil? result) (number? result)) result (seq result)))

(defmacro deftesteq
  "Define a function 'name' that takes a set of hinted array arguments, and tests that when
   executed in a scope with arg symbols bound to fresh copies of the input arrays, all exprs
   return equivalent values and have the same side effects on input arrays"
  [name args expr & exprs]
  (let [base-args (gensym "base-args")
        base-result (gensym "base-result")
        test-expr (gensym "test-expr")]
    `(defn ~name ~args
       (test/testing ~(clojure.core/name name)
         (let [{~base-args :args ~base-result :result} (expr-results ~args ~expr)]
           (if-let [e# (:exception (:results ~base-result))]
             (test/is (not e#) "Baseline expr threw an exception")
             (do ~@(for [test-expr exprs]
                     `(let [{expr# :expr args# :args result# :result} (expr-results ~args ~test-expr)]
                        (test/testing (str "in expr " expr#)
                          (if-let [e# (:exception (:results result#))]
                            (test/is (not e#) "expr threw an exception")
                            (do (doseq [[i# base-arg# arg#] (map vector (range) ~base-args args#)]
                                  (test/testing (format "%sth inputs are equal after the op" i#)
                                    (test/is (= (=-able base-arg#) (=-able arg#)))))
                                (test/testing "return values are equivalent"
                                  (test/is (= (=-able ~base-result) (=-able result#))))))))))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generating tests and tables for performance of benchmark forms

(defmacro bench-results [arg-syms [slowness expr]]
  `(let ~(clone-args arg-syms)
     {:expr (quote ~expr)
      :slowness ~slowness
      :results (result-or-ex (criterium/quick-benchmark ~expr {}))}))

(defn mean-time [bench-result]
  (-> bench-result :results :mean first))

(defn slowness [baseline-result bench-result]
  (/ (mean-time bench-result) (mean-time baseline-result)))

(defn print-benchmark-results
  "Pretty-print a benchmark comparison."
  [bench-data]
  (let [baseline (first bench-data)]
    (pprint/print-table
     [:expr :slowness :ms :variance]
     (for [benchmark bench-data]
       {:expr (:expr benchmark)
        :slowness (format "%f" (slowness baseline benchmark))
        :ms (->> benchmark :results :mean first (* 1e3) (format "%f"))
        :variance (->> benchmark :results :variance first
                       (* 1e3) (format "%f"))}))
    (println)))

(defn test-benchmark-results [[baseline & results]]
  (doseq [result results]
    (when-let [expected (:slowness result)]
      (test/is (< (slowness baseline result) expected)
               (str "expr " (:expr result) " was too slow")))))

(defmacro deftestfast
  "Define a function 'name' that takes a set of hinted array arguments, and tests that when
   executed in a scope with arg symbols bound to fresh copies of the input arrays,
   all exprs in slowness-and-exprs [slowness1 expr1 slowness2 expr2 ...]
   execute at most 'slowness' times as slowly as the baseline 'expr'.

   Also prints a table of the results of the benchmarks."
  [name args expr & slowness-and-exprs]
  (let []
    `(defn ~name ~args
       (test/testing ~(clojure.core/name name)
         (binding [criterium/*final-gc-problem-threshold* 0.4]
           (let [raw-results# [~@(for [s-and-e (cons [{} expr] (partition 2 slowness-and-exprs))]
                                   `(bench-results ~args ~s-and-e))]
                 results# (keep (fn [result#]
                                  (if-let [e# (:exception (:results result#))]
                                    (do (test/is (not e#) (format "expr %s threw an exception"
                                                                  (:expr result#)))
                                        nil)
                                    result#))
                                raw-results#)]
             (print-benchmark-results results#)
             (test-benchmark-results results#)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Defbenchmark macro for defining benchmarks

(defn test-name [n] (symbol (str "test-" (name n))))
(defn bench-name [n] (symbol (str "bench-" (name n))))

(defmacro defbenchmark
  "Declare a named benchmark from a list [expr slowness2 expr2 slowness3 expr3 ...].

   Generates two functions, 'test-<name>' and 'bench-<name>', which respectively
   test the equivalence of the expressions and test their speed, given any number
   of array arguments.  See 'deftesteq' and 'deftestfast' for details."
  [name args expr & slowness-and-exprs]
  (assert (even? (count slowness-and-exprs)))
  `(do (deftesteq ~(test-name name) ~args ~expr ~@(map second (partition 2 slowness-and-exprs)))
       (deftestfast ~(bench-name name) ~args ~expr ~@slowness-and-exprs)))

(set! *warn-on-reflection* false)
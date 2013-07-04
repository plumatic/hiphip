;;; Benchmark code shared between array types.
;; Assumes the appropriate hiphip array type ns has been aliased as 'hiphip',
;; and the appropriate Java baseline class has been imported as 'JavaBaseline'

(require '[clojure.test :as test])

(set! *unchecked-math* true)

(def type-info hiphip/type-info)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generic helpers

(defmacro gen-array
  "Generate an array of the correct type, consisting of a short repeating sequence
   of small integers of length 'size' with given 'phase'."
  [size phase]
  `(~(:constructor type-info)
    (take ~size (drop ~phase (apply concat (repeat [-2 3 0 -1 0 1 -1 2 3]))))))

(defmacro result-or-ex [form]
  `(try ~form (catch Exception e# {:exception e#})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generating tests for equivalence of benchmark forms

(defmacro expr-results [expr]
  `(let [~'xs (gen-array ~'size 0)
         ~'ys (gen-array ~'size 1)]
     {:expr (quote ~expr)
      :xs ~'xs
      :ys ~'ys
      :result (result-or-ex ~expr)}))

(defn- =-able
  "Convert result into something that can be tested for value equality"
  [result]
  (if (or (nil? result) (number? result)) result (seq result)))

(defmacro deftesteq
  "Define a function 'name' that takes a single argument 'size', and tests that
   when executed in a scope with 'size bound to size, and 'xs and 'ys bound to arrays
   of size 'size', all exprs return equivalent values and have the same side effects
   on the 'xs and 'ys."
  [name expr & exprs]
  (let [base-xs (gensym "base-xs")
        base-ys (gensym "base-ys")
        base-result (gensym "base-result")
        test-expr (gensym "test-expr")]
    `(defn ~name [^long ~'size]
       (let [{~base-xs :xs ~base-ys :ys ~base-result :result} (expr-results ~expr)]
         (if-let [e# (:exception ~base-result)]
           (is (not e#) "Baseline expr threw an exception")
           (do ~@(for [test-expr exprs]
                   `(let [{expr# :expr xs# :xs ys# :ys result# :result} (expr-results ~test-expr)]
                      (test/testing (str "in expr " expr#)
                        (if-let [e# (:exception result#)]
                          (is (not e#) "expr threw an exception")
                          (do (testing "first inputs are equal after the op"
                                (test/is (= (seq ~base-xs) (seq xs#))))
                              (testing "second inputs are equal after the op"
                                (test/is (= (seq ~base-ys) (seq ys#))))
                              (testing "return values are equivalent"
                                (test/is (= (=-able ~base-result) (=-able result#)))))))))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generating tests and tables for performance of benchmark forms

(defmacro bench-results [[slowness expr]]
  `(let [~'xs (gen-array ~'size 0)
         ~'ys (gen-array ~'size 1)]
     {:expr (quote ~expr)
      :slowness ~slowness
      :results (result-or-ex
                (bench/quick-benchmark
                 ~expr
                 {} #_ {:samples 3
                        :target-execution-time 100000000
                        :warmup-jit-period 500000000}))}))

(defn mean-time [bench-result]
  (-> bench-result :results :mean first))

(defn slowness [baseline-result bench-result]
  (/ (mean-time bench-result) (mean-time baseline-result)))

(defn expected-slowness [result]
  (when-let [slowness (:slowness result)]
    (if (number? slowness)
      slowness
      (let [k (keyword (name (:etype type-info)))]
        (assert (contains? slowness k))
        (slowness k)))))

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
    (println (slowness baseline result) (expected-slowness result))
    (when-let [expected (expected-slowness result)]
      (is (< (slowness baseline result) expected)
          (str "expr " (:expr result) " was too slow")))))

(defmacro deftestfast
  "Define a function 'name' that takes a single argument 'size', and tests that
   when executed in a scope with 'size bound to size, and 'xs and 'ys bound to arrays
   of size 'size', all exprs in slowness-and-exprs [slowness1 expr1 slowness2 expr2 ...]
   execute at most 'slowness' times as slowly as the baseline 'expr'.

   Each slowness can be a single number, or a map
   {:double slowness :float slowness :int slowness :long slowness}
   that expresses the expected slowness for each array type.

   Also prints a table of the results of the benchmarks."
  [name expr & slowness-and-exprs]
  (let []
    `(defn ~name [^long ~'size]
       (binding [bench/*final-gc-problem-threshold* 0.4]
         (let [raw-results# [~@(for [s-and-e (cons [{} expr] (partition 2 slowness-and-exprs))]
                                 `(bench-results ~s-and-e))]
               results# (keep (fn [result#]
                                (if-let [e# (:exception result#)]
                                  (do (is (not e#) (format "expr %s threw an exception"
                                                           (:expr result#)))
                                      nil)
                                  result#))
                              raw-results#)]
           (print-benchmark-results results#)
           (test-benchmark-results results#))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Defbenchmark macro for defining benchmarks

(def +all-tests+ (atom {}))
(def +all-benchmarks+ (atom {}))

(defn test-name [n] (symbol (str "test-" (name n))))
(defn bench-name [n] (symbol (str "bench-" (name n))))


(defmacro defbenchmark
  "Declare a named benchmark from a list [expr slowness2 expr2 slowness3 expr3 ...].

   Generates two functions, 'test-<name>' and 'bench-<name>', which respectively
   test the equivalence of the expressions and test their speed, given a single
   array size argument.  See 'deftesteq' and 'deftestfast' for details.

   Expressions should be based on arrays 'xs' and 'ys' that will be provided by the
   benchmark scaffolding code, and can also refer to 'size' for the array size."
  [name expr & slowness-and-exprs]
  (assert (even? (count slowness-and-exprs)))
  `(do (deftesteq ~(test-name name) ~expr ~@(map second (partition 2 slowness-and-exprs)))
       (swap! +all-tests+ assoc '~name ~(test-name name))
       (deftestfast ~(bench-name name) ~expr ~@slowness-and-exprs)
       (swap! +all-benchmarks+ assoc '~name ~(bench-name name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Specific benchmarks

(defbenchmark aclone
  (JavaBaseline/aclone xs)
  1.1 (hiphip/aclone xs))

(defbenchmark alength
  (JavaBaseline/alength xs)
  1.1 (hiphip/alength xs)
  ;; failure cases for testing the tests
  ;; 1.2 (inc (hiphip/alength xs))
  ;; 1.2 (do (aset xs 0 100.0) (hiphip/alength xs))
  ;; 0.3 (do (aset ys 0 101.0) (hiphip/alength xs))
  )

(defbenchmark aget
  (JavaBaseline/aget xs 0)
  1.1 (hiphip/aget xs 0))

(defbenchmark aset
  (JavaBaseline/aset xs 0 42)
  1.1 (hiphip/aset xs 0 42))

(defbenchmark ainc
  (JavaBaseline/ainc xs 0 1)
  1.1 (hiphip/ainc xs 0 1))

(defmacro hinted-hiphip-areduce [bind ret-sym init final]
  `(hiphip/areduce ~bind ~ret-sym (~(:etype type-info) ~init) ~final))

(defmacro hinted-clojure-areduce [arr-sym idx-sym ret-sym init final]
  `(areduce ~arr-sym ~idx-sym ~ret-sym (~(:etype type-info) ~init) ~final))

(defbenchmark areduce-and-dot-product
  (JavaBaseline/dot_product xs ys)
  1.1 (hinted-hiphip-areduce [x xs y ys] ret 0 (+ ret (* x y)))
  1.1 (hiphip/dot-product xs ys)
  nil (hinted-clojure-areduce xs i ret 0 (+ ret (* (aget xs i) (aget ys i))))
  nil (reduce + (map * xs ys)))

(defbenchmark doarr-and-afill!
  (JavaBaseline/multiply_in_place_pointwise xs ys)
  1.1 (do (hiphip/doarr [[i x] xs y ys] (hiphip/aset xs i (* x y))) xs)
  1.1 (hiphip/afill! [x xs y ys] (* x y)))

(defbenchmark afill!
  (JavaBaseline/multiply_in_place_by_idx xs)
  1.1 (hiphip/afill! [[i x] xs] (* x i)))

(defbenchmark amake
  (JavaBaseline/acopy_inc size xs)
  1.1 (hiphip/amake [i size] (inc (hiphip/aget xs i))))

(defbenchmark amap
  (JavaBaseline/amap_inc xs)
  1.1 (hiphip/amap [x xs] (inc x))
  nil (amap xs i ret (aset ret i (inc (aget xs i)))))

(defbenchmark amap-with-index
  (JavaBaseline/amap_plus_idx xs)
  1.1 (hiphip/amap [[i x] xs] (+ i x)))

(defbenchmark asum
  (JavaBaseline/asum xs)
  1.1 (hiphip/asum xs)
  nil (hinted-clojure-areduce xs i ret 0 (+ ret (aget xs i)))
  nil (reduce + xs))

(defbenchmark asum-op
  (JavaBaseline/asum_square xs)
  1.1 (hiphip/asum [x xs] (* x x)))

(defbenchmark aproduct
  (JavaBaseline/aproduct xs)
  1.1 (hiphip/aproduct xs))

(defbenchmark amax
  (JavaBaseline/amax xs)
  ;; amax is inexplicably slower with *unchecked-math* on...
  1.7 (hiphip/amax xs))

(defbenchmark amin
  (JavaBaseline/amin xs)
  ;; amax is inexplicably slower with *unchecked-math* on...
  1.7 (hiphip/amin xs))

(defbenchmark amin
  (JavaBaseline/amean xs)
  1.1 (hiphip/amean xs))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Top-level tests

(defn test-equivalence [size]
  (doseq [[n test] @+all-tests+]
    (testing (name n) (test size))))

(defn test-performance [size]
  (printf "Benchmarking  %s arrays with %s elements (this may take awhile)\n"
          (name (:etype type-info)) size)
  (doseq [[n test] @+all-benchmarks+]
    (testing (name n) (test size))))

(deftest big-array-equivalence
  (test-equivalence 10000))

(deftest ^:benchmark big-array-perf
  (test-performance 10000))

(comment
  (deftest ^:benchmark small-array-perf
    (test-performance 10)))

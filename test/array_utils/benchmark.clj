(ns ^{:doc "Benchmarking suite for array-utils"
      :author "EHF"}
  array-utils.benchmark
  (:use clojure.test)
  (:require [array-utils.double :as d]
            [array-utils.long :as l]
            [array-utils.generators :as gen]
            [criterium.core :as bench]
            [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]])
  (:import benchmark.JavaBaseline))

;; TODO: mess with bench/*final-gc-problem-threshold* for fewer warnings.

(defmacro run-benchmarks
  "Run and compare a set of benchmarks, returning descriptive maps of
  the forms and results.

  This is a macro so you don't have to quote the benchmark forms."
  [& exprs]
  (let [bench-sym `bench/quick-benchmark]
    (vec (for [[options expr] (partition 2 exprs)]
           `{:form (quote ~expr)
             :options ~options
             :results (~bench-sym ~expr)}))))

(defn benchmarks
  []
  (let [^doubles xs (gen/darray 10000)
        ^doubles ys (gen/darray 10000)]
    [(delay (run-benchmarks
              {} (JavaBaseline/asum_noop xs)
              {:expected-slowness 1.1} (d/asum [a xs] a)))
     (delay (run-benchmarks
              {} (JavaBaseline/asum_op xs)
              {:expected-slowness 1.1} (d/asum [a xs] (+ 1.0 (* 2.0 a)))
              {} (areduce xs i ret (double 0)
                          (+ ret (+ 1.0 (* 2.0 (aget xs i)))))))
     (delay (run-benchmarks
              {} (JavaBaseline/afill_op xs)
              ;; Operations with ints are slow, not sure why. This could be a
              ;; major point of improvement.
              {:expected-slowness 2.4} (d/afill! [[i a] xs] (+ 1.0 (* 2.0 i)))
              {} (dotimes [i (alength xs)] (aset xs i (+ 1.0 (* 2.0 i))))))
     (delay (run-benchmarks
              ;; For some reason this Java optimizes to *very fast*,
              ;; faster than an areduce. I'm not sure why.
              {} (JavaBaseline/amap_inplace_op xs)
              {:expected-slowness 1.4} (d/afill! [a xs] (* 2.0 a))
              {:expected-slowness 1.8} (d/afill! [a xs] (+ 1.0 (* 2.0 a)))
              {} (dotimes [i (alength xs)]
                   (aset xs i (+ 1.0 (* 2.0 (aget xs i)))))
              ;; Operations with ints are slow, not sure why.
              {:expected-slowness 2.4} (d/afill! [[i a] xs] (+ i (* 2.0 a)))))
     (delay (run-benchmarks
              {} (JavaBaseline/aclone xs)
              {:expected-slowness 1.4} (d/amap [[i a] xs] a)))
     (delay (run-benchmarks
              {} (JavaBaseline/amap_op xs)
              {:expected-slowness 1.4} (d/amap [[i a] xs] (+ 1.0 (* 2.0 a)))))
     (delay (run-benchmarks
              {} (JavaBaseline/dot_product xs ys)
              {:expected-slowness 1.5} (d/dot-product xs ys)))]))

(defn reformat-benchmark
  [baseline benchmark]
  {:form (:form benchmark)
   :slowness (format "%f"
                     (/ (-> benchmark :results :mean first)
                        (-> baseline :results :mean first)))
   :ms (->> benchmark :results :mean first (* 1e3) (format "%f"))
   :variance (->> benchmark :results :variance first (* 1e3) (format "%f"))})

(defn print-benchmark
  [bench-data]
  (pprint/print-table
    [:form :slowness :ms :variance]
    (map (partial reformat-benchmark (first bench-data)) bench-data)))

(deftest benchmarks-test
  (testing "Benchmarks are appropriately fast.")
  (doseq [benchmark (benchmarks)]
    (let [baseline (first @benchmark)]
      (doseq [result @benchmark]
        (when-let [expected-slowness
                   (get-in result [:options :expected-slowness])]
          (let [slowness (/ (-> result :results :mean first)
                            (-> baseline :results :mean first))]
            (is (< slowness expected-slowness)
              (format "%s was too slow! %s * baseline > %s * baseline"
                      (:form result)
                      slowness expected-slowness))))))))

(defn -main [& {:keys [dest] :or {dest "benchmarks"}}]
  (println "Benchmarking. This might take a while.")
  (doseq [benchmark (benchmarks)]
    (print-benchmark @benchmark)))

(ns ^{:doc "Benchmarking suite for hiphip"
      :author "EHF"}
  hiphip.benchmark
  (:use clojure.test)
  (:require [hiphip.double :as d]
            [hiphip.long :as l]
            [hiphip.core :as core]
            [hiphip.generators :as gen]
            [criterium.core :as bench]
            [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]])
  (:import hiphip.benchmark.JavaBaseline))

(defmacro benchmark
  "Declare a benchmark, a list [options form options form ...]"
  [& exprs]
  (assert (even? (count exprs)))
  `(delay
    (binding [bench/*final-gc-problem-threshold* 0.2]
      [~@(for [[options expr] (partition 2 exprs)]
           `{:form (quote ~expr)
             :options ~options
             :results (bench/quick-benchmark ~expr {})})])))


(defn benchmarks
  "Create a sequence of delays that run benchmarks. This uses delays so
  we can show some results before all benchmarks are complete."
  []
  (let [^doubles xs (gen/darray 10000)
        ^doubles ys (gen/darray 10000)]
    [(benchmark
      {} (JavaBaseline/asum_noop xs)
      {:expected-slowness 1.1} (d/asum [a xs] a)
      {} (reduce + xs))
     (benchmark
      {} (JavaBaseline/asum_op xs)
      {:expected-slowness 1.1} (d/asum [a xs] (+ 1.0 (* 2.0 a)))
      {} (areduce xs i ret (double 0)
                  (+ ret (+ 1.0 (* 2.0 (aget xs i)))))
      {} (reduce + (map (fn [x] (+ 1.0 (* 2.0 x))) xs)))
     (benchmark
      {} (JavaBaseline/afill_inc xs)
      {:expected-slowness 1.8} (d/afill! [a xs] (+ 1.0 a))
      {:expected-slowness 1.8} (d/doarr [[i a] xs] (aset xs i (+ 1.0 a)))
      {:expected-slowness 1.8} (core/dotimes-int [i 0 (alength xs)] (JavaBaseline/ainc xs i)))
     (benchmark
      {} (JavaBaseline/afill_value_op xs)
      {:expected-slowness 1.8} (d/afill! [a xs] (+ 1.0 (* 2.0 a)))
      {} (dotimes [i (alength xs)] (aset xs i (+ 1.0 (* 2.0 (aget xs i))))))
     (benchmark
      {} (JavaBaseline/afill_index_op xs)
      ;; Operations with ints are slow, not sure why. This could be a
      ;; major point of improvement.
      {:expected-slowness 4.5} (d/afill! [[i a] xs] (+ 1.0 (* 2.0 i)))
      {} (dotimes [i (alength xs)] (aset xs i (+ 1.0 (* 2.0 i)))))
     (benchmark
      {} (JavaBaseline/aclone xs)
      {:expected-slowness 1.4} (d/amap [[i a] xs] a)
      {} (aclone xs))
     (benchmark
      {} (JavaBaseline/amap_op xs)
      {:expected-slowness 1.4} (d/amap [[i a] xs] (+ 1.0 (* 2.0 a)))
      {} (doall (map (fn [x]  (+ 1.0 (* 2.0 x))) xs)))
     (benchmark
      {} (JavaBaseline/dot_product xs ys)
      {:expected-slowness 1.5} (d/dot-product xs ys)
      {} (areduce xs i ret 0.0
                  (+ ret (* (aget xs i)
                            (aget ys i))))
      {} (reduce + (map * xs ys)))
     (benchmark
      {} (JavaBaseline/amean xs)
      {:expected-slowness 1.1} (d/amean xs))
     (benchmark
      {} (JavaBaseline/amax xs)
      ;; amax is inexplicably slower with *unchecked-math* on...
      {:expected-slowness 1.7} (d/amax xs))]))

(defn print-benchmark
  "Pretty-print a benchmark comparison."
  [bench-data]
  (let [baseline (first bench-data)]
    (pprint/print-table
     [:form :slowness :ms :variance]
     (for [benchmark bench-data]
       {:form (:form benchmark)
        :slowness (format "%f"
                          (/ (-> benchmark :results :mean first)
                             (-> baseline :results :mean first)))
        :ms (->> benchmark :results :mean first (* 1e3) (format "%f"))
        :variance (->> benchmark :results :variance first
                       (* 1e3) (format "%f"))}))))

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
                (format "%s was too slow!"
                        (:form result)
                        slowness expected-slowness))))))))

(defn -main
  []
  (println "Benchmarking. This might take a while.")
  (doseq [benchmark (benchmarks)]
    (print-benchmark @benchmark)))

(ns hiphip.array-test
  "Benchmarks and tests for the generic array macros in hiphip.array"
  (:use clojure.test hiphip.test-utils)
  (:require
   [hiphip.impl.core :as impl]
   [hiphip.array :as array])
  (:import hiphip.Baseline))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Miscellaneous extra tests

(deftest test-ranges
  (is (= 7.0 (array/areduce [:range [3 5] a (double-array (range 10))] ret 0.0 (+ ret a))))
  (is (= 7.0
         (let [res (atom 0.0)]
           (array/doarr [:range [3 5] a (double-array (range 10))] (swap! res + a ))
           @res)))
  (is (= [5 6]
         (seq (array/amap long [:range [3 5] a (double-array (range 10))] (+ (long a) 2)))))
  (is (= [0.0 1.0 2.0 5.0 6.0 5.0 6.0 7.0 8.0 9.0]
         (seq (array/afill! double [:range [3 5] a (double-array (range 10))] (+ a 2))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Benchmark/equality tests

(defbenchmark make-double-array [size]
  (Baseline/make-double-array size)
  1.1 (array/make-array double size)
  nil (double-array size))

(defbenchmark make-string-array [size]
  (Baseline/make-string-array size)
  1.1 (array/make-array String size)
  nil (make-array String size))

(defbenchmark make-double-array-and-fill [size]
  (Baseline/make-double-array-and-fill size)
  1.3 (let [a (array/make-array double size)]
        (impl/dotimes-int [i size] (aset a i (double i)))
        a)
  1.3 (array/amake double [i size] (double i))
  nil (double-array (range size)))

(defbenchmark make-long-array-and-fill [size]
  (Baseline/make-long-array-and-fill size)
  1.1 (let [a (array/make-array long size)]
        (impl/dotimes-int [i size] (aset a i i))
        a)
  1.1 (array/amake long [i size] i)
  nil (long-array (range size)))

(defbenchmark make-string-array-and-fill [size]
  (Baseline/make-string-array-and-fill size)
  1.1 (let [a (array/make-array String size)]
        (impl/dotimes-int [i size] (aset a i "test"))
        a)
  1.1 (array/amake String [i size] "test")
  nil (into-array String (repeat size "test")))

(defbenchmark areduce-dl [^doubles xs ^longs ys]
  (Baseline/areduce_dl xs ys)
  2.0 (array/areduce [x xs y ys] ret 0.0 (+ ret (* x y)))
  nil (areduce xs i ret 0.0 (+ ret (* (aget xs i) (aget ys i))))
  nil (reduce + (map * xs ys)))

(defbenchmark multiply-pointwise-dl [^doubles xs ^longs ys]
  (Baseline/multiply_pointwise_dl xs ys)
  1.3 (array/amap double [x xs y ys] (* x y)))

(defbenchmark multiply-in-place-pointwise-dl [^doubles xs ^longs ys]
  (Baseline/multiply_in_place_pointwise_dl xs ys)
  2.1 (array/afill! double [x xs y ys] (* x y)))

(set! *unchecked-math* true)

(defbenchmark multiply-in-place-pointwise-dl-unchecked [^doubles xs ^longs ys]
  (Baseline/multiply_in_place_pointwise_dl xs ys)
  1.5 (array/afill! double [x xs y ys] (* x y)))

(set! *unchecked-math* false)

(defbenchmark fill-string-pointwise-product-dl [^{:tag "[Ljava.lang.String;"} os ^doubles xs ^longs ys]
  (Baseline/fill_string_pointwise_product_dl os xs ys)
  1.2 (array/afill! String [_ os x xs y ys] (str (* x y))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Top-level benchmark/equality test runners

(defmacro gen-array [type size phase]
  `(array/amake ~type [i# ~size] (nth [-2 3 0 -1 0 1 -1 2 3] (mod (+ i# ~phase) 9))))

(defn all-tests [size]
  (test-make-double-array size)
  (test-make-string-array size)
  (test-make-double-array-and-fill size)
  (test-make-long-array-and-fill size)
  (test-make-string-array-and-fill size)
  (test-areduce-dl (gen-array double size 0) (gen-array long size 1))
  (test-multiply-pointwise-dl (gen-array double size 0) (gen-array long size 1))
  (test-multiply-in-place-pointwise-dl (gen-array double size 0) (gen-array long size 1))
  (test-multiply-in-place-pointwise-dl-unchecked (gen-array double size 0) (gen-array long size 1))
  (test-fill-string-pointwise-product-dl
   (make-array String size) (gen-array double size 0) (gen-array long size 1)))

(deftest hiphip-array-test
  (all-tests 1000))

(defn all-benches [size]
  (bench-make-double-array size)
  (bench-make-string-array size)
  (bench-make-double-array-and-fill size)
  (bench-make-long-array-and-fill size)
  (bench-make-string-array-and-fill size)
  (bench-areduce-dl (gen-array double size 0) (gen-array long size 1))
  (bench-multiply-pointwise-dl (gen-array double size 0) (gen-array long size 1))
  (bench-multiply-in-place-pointwise-dl (gen-array double size 0) (gen-array long size 1))
  (bench-multiply-in-place-pointwise-dl-unchecked (gen-array double size 0) (gen-array long size 1))
  (bench-fill-string-pointwise-product-dl
   (make-array String size) (gen-array double size 0) (gen-array long size 1)))

(deftest ^:bench hiphip-array-bench
  (all-benches 10000))

(set! *warn-on-reflection* false)
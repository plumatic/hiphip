(ns array-utils.generative.tests
  (:use array-utils.generators)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [clojure.data.generators :as gen]
            [clojure.test.generative.event :as event] ;; UI console
            [clojure.test.generative.runner :as runner]
            [array-utils.double :as d]
            [array-utils.long :as l]))

(defn double? [n] (instance? Double n))

(defn long? [n] (instance? Long n))

;; # Tests

;; ## Doubles

(defspec asum-sums-double
  (fn [^doubles xs]
    (d/asum [x xs] x))
  [^{:tag (`darray 10e3)} xs]
  (assert (= % (apply + xs))))

(defspec asum-sums-bounded-double
  (fn [^doubles xs]
    (d/asum [x xs :range [50 100]] x))
  [^{:tag (`darray 10e3)} xs]
  (assert (= % (apply + (take 50 (drop 50 xs))))))

(defspec dot-product-double
  d/dot-product
  [^{:tag (`darray 10e3)} a ^{:tag (`darray 10e3)} b]
  (do
    (assert (double? %))
    (assert (= % (reduce + (map * a b))))))

(defspec amean-double-returns-mean
  d/amean
  [^{:tag (`darray 10e3)} a]
  (assert (= (/ (reduce + a) (count a)) %)))

(defspec amax-returns-the-largest-double
  d/amax
  [^{:tag (`darray 10e3)} a]
  (assert (= (reduce max a) %)))

(defspec amin-returns-the-smallest-double
  d/amin
  [^{:tag (`darray 10e3)} a]
  (assert (= (reduce min a) %)))

(defspec amap-maps-doubles
  (fn [^doubles xs]
    (d/amap [[i x] xs] (+ i x)))
  [^{:tag (`drange 10e3)} xs]
  (do
    (assert (every? true? (map == xs (range 10e3))))
    (assert (every? true? (map == % (for [i (range 10e3)] (* 2 i)))))))

(defspec afill!-replaces-doubles-in-place
  (fn [^doubles xs]
    (let [ys (double-array (alength xs))]
      (d/afill! [[i y] ys]
                (aget ^doubles xs i))
      ys))
  [^{:tag (`darray 10e3)} xs]
  (assert (every? true? (map = xs %))))

(defspec afill!-only-mutates-the-first-darray
  (fn [^doubles xs]
    (let [old-xs (aclone xs)
          ys (double-array (alength xs))]
      (d/afill! [[i y] ys b xs] b)
      old-xs))
  [^{:tag (`darray 10e3)} xs]
  (assert (every? true? (map = xs %))))

(defspec afill!-bounded-replaces-double-interval
  (fn [^doubles xs]
    (d/afill! [x xs :range [1 4]] 2)
    xs)
  [^{:tag (`drange 10e3)} xs]
  (assert (every? true? (map == (concat [0.0 2.0 2.0 2.0] (range 4 10e3)) xs))))

(defspec double-doarr-has-side-effects
  (fn [^doubles xs]
    (let [ys (double-array (alength xs))]
      (d/doarr [[i y] ys x xs]
               (aset-double ys i x))
      ys))
  [^{:tag (`darray 10e3)} xs]
  (assert (every? true? (map = xs %))))

;; ----------------------------------------------------------------------

;; ## Longs

(defspec asum-sums-long
  (fn [^longs xs]
    (l/asum [x xs] x))
  [^{:tag (`larray 10e3)} xs]
  (assert (= % (apply + xs))))

(defspec asum-sums-bounded-long
  (fn [^longs xs]
    (l/asum [x xs :range [50 100]] x))
  [^{:tag (`larray 10e3)} xs]
  (assert (= % (apply + (take 50 (drop 50 xs))))))

(defspec dot-product-long
  l/dot-product
  [^{:tag (`larray 100 0 10e3)} a ^{:tag (`larray 100 0 10e3)} b]
  (do
    (assert (long? %))
    (assert (= % (reduce + (map * a b))))))

(defspec amean-long-returns-mean
  l/amean
  [^{:tag (`larray 10e3)} a]
  (assert (= (/ (reduce + a) (count a)) %)))

(defspec amax-returns-the-largest-long
  l/amax
  [^{:tag (`larray 10e3)} a]
  (assert (= (reduce max a) %)))

(defspec amin-returns-the-smallest-long
  l/amin
  [^{:tag (`larray 10e3)} a]
  (assert (= (reduce min a) %)))

(defspec amap-maps-longs
  (fn [^longs xs]
    (l/amap [[i x] xs] (+ i x)))
  [^{:tag (`lrange 10e3)} xs]
  (do
    (assert (every? true? (map = xs (range 10e3))))
    (assert (every? true? (map = % (for [i (range 10e3)] (* 2 i)))))))

(defspec afill!-replaces-longs-in-place
  (fn [^longs xs]
    (let [ys (long-array (alength xs))]
      (l/afill! [[i y] ys]
                (aget ^longs xs i))
      ys))
  [^{:tag (`larray 10e3)} xs]
  (assert (every? true? (map = xs %))))

(defspec afill!-only-mutates-the-first-larray
  (fn [^longs xs]
    (let [old-xs (aclone xs)
          ys (long-array (alength xs))]
      (l/afill! [[i y] ys b xs] b)
      old-xs))
  [^{:tag (`larray 10e3)} xs]
  (assert (every? true? (map = xs %))))

(defspec afill!-bounded-replaces-long-interval
  (fn [^longs xs]
    (l/afill! [x xs :range [1 4]] 2)
    xs)
  [^{:tag (`lrange 10e3)} xs]
  (assert (every? true? (map == (concat [0 2 2 2] (range 4 10e3)) xs))))

(defspec long-doarr-has-side-effects
  (fn [^longs xs]
    (let [ys (long-array (alength xs))]
      (l/doarr [[i y] ys x xs]
               (aset-long ys i x))
      ys))
  [^{:tag (`larray 10e3)} xs]
  (assert (every? true? (map = xs %))))

(defn -main []
  (runner/-main "test/array_utils/generative"))

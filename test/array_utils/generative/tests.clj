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

;; We want an even? that works on floats.
(defn even? [n] (zero? (mod n 2)))

(def double1 (double-array (range 500 1000)))

(def long1 (long-array (range 500 1000)))

;; # Tests

;; TODO: Write a cool test for filter!

;; ----------------------------------------------------------------------

;; ## Doubles

(defspec dot-product-returns-double
  d/dot-product
  [^{:tag (`darray 10e3)} a ^{:tag (`darray 10e3)} b]
  (assert (double? %)))

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

(defspec afill-bounded!-replaces-double-interval
  (fn [^doubles xs]
    (d/afill-bounded! [1 4] [x xs] 2)
    xs)
  [^{:tag (`darray 10e3)} xs]
  (assert (every? true? (map == [2.0 2.0 2.0] (take 3 (rest xs))))))

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

(defspec dot-product-returns-long
  l/dot-product
  [^{:tag (`larray 100 0 10e3)} a ^{:tag (`larray 100 0 10e3)} b]
  (assert (long? %) (str "wrong type" (type %))))

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

(defspec afill-bounded!-replaces-long-interval
  (fn [^longs xs]
    (l/afill-bounded! [1 4] [x xs] 2)
    xs)
  [^{:tag (`larray 10e3)} xs]
  (assert (every? true? (map == [2 2 2] (take 3 (rest xs))))))

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

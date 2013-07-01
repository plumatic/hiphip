(ns hiphip.generators
  (:require [clojure.data.generators :as gen]))

;; # Generators

;; TODO: Add more varieties?

;; Doubles

(defn darray
  (^doubles [] (darray 10e3))
  (^doubles [size] (gen/double-array gen/double size)))

(defn drange
  (^doubles [] (double-array (range 10e3)))
  (^doubles [size] (double-array (range size))))

;; Floats

(defn farray
  (^floats [] (farray 10e3))
  (^floats [size] (gen/float-array gen/float size)))

(defn frange
  (^floats [] (float-array (range 10e3)))
  (^floats [size] (float-array (range size))))

;; Ints

(defn iarray
  (^ints [] (iarray 10e3))
  ;; to avoid overflow when calculating the dot-product
  (^ints [size] (gen/int-array (constantly 1) size)))

(defn irange
  (^ints [] (int-array (range 10)))
  (^ints [size] (int-array (range size))))

;; Longs

(defn larray
  (^longs [] (larray 10e3))
  (^longs [size] (larray size 0 10e3))
  (^longs [size hi lo] (gen/long-array (gen/uniform hi lo) size)))

(defn lrange
  (^longs [] (long-array (range 10e3)))
  (^longs [size] (long-array (range size))))


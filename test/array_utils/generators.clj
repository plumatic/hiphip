(ns array-utils.generators
  (:require [clojure.data.generators :as gen]))

;; # Generators

;; TODO: Add more varieties?

(defn darray
  (^doubles [] (darray 10e3))
  (^doubles [size]
      (gen/double-array gen/double size)))

(defn larray
  (^longs [] (larray 10))
  (^longs [size] (larray size 0 10e3))
  (^longs [size hi lo]
     (gen/long-array (gen/uniform hi lo) size)))

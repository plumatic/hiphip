(ns array-utils.generators
  (:require [clojure.data.generators :as gen]))

;; # Generators

;; TODO: Add more varieties?

(defn darray
  ([] (darray 10e3))
  ([size]
      (gen/double-array gen/double size)))

(defn larray
  ([] (larray 10))
  ([size] (larray size 0 10e3))
  ([size hi lo]
     (gen/long-array (gen/uniform hi lo) size)))

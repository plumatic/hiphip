(ns hiphip.generative.int
  (:use hiphip.int hiphip.generative.type_impl_gen)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [hiphip.generators :as gen])
  (:refer-clojure :exclude [amap areduce alength aclone aset aget]))

(def array-gen #(gen/iarray 10e2))
(def range-gen #(gen/irange 10e2))
(def new-array int-array)
(defn is-type? [n] (instance? Integer n))

(eval (read-string (load-type-impl-gen)))
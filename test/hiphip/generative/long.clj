(ns hiphip.generative.long
  (:use hiphip.long)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [hiphip.generators :as gen])
  (:refer-clojure :exclude [amap areduce alength aclone aset aget]))

(def array-gen #(gen/larray 10e3))
(def range-gen #(gen/lrange 10e3))
(def new-array long-array)
(defn is-type? [n] (instance? Long n))

(load "type_impl_gen")
(ns array-utils.generative.int
  (:use array-utils.int)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [array-utils.generators :as gen])
  (:refer-clojure :exclude [amap areduce alength aclone aset]))

(def array-gen #(gen/iarray 10e2))
(def range-gen #(gen/irange 10e2))
(def new-array int-array)
(defn is-type? [n] (instance? Integer n))

(load "common")
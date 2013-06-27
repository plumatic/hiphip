(ns array-utils.generative.float
  (:use array-utils.float)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [array-utils.generators :as gen])
  (:refer-clojure :exclude [amap areduce alength aclone aset]))

(def array-gen #(gen/farray 10e1))
(def range-gen #(gen/frange 10e1))
(def new-array float-array)
(defn is-type? [n] (instance? Float n))

(load "common")
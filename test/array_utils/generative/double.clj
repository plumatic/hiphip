(ns array-utils.generative.double
  (:use array-utils.double)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [array-utils.generators :as gen])
  (:refer-clojure :exclude [amap areduce alength aclone aset]))

(def array-gen #(gen/darray 10e3))
(def range-gen #(gen/drange 10e3))
(def new-array double-array)
(defn is-type? [n] (instance? Double n))

(load "common")
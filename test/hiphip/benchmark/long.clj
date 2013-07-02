(ns hiphip.benchmark.long
  "Benchmarks for double arrays"
  (:use clojure.test)
  (:require
   [hiphip.long :as hiphip]
   [criterium.core :as bench]
   [clojure.pprint :as pprint])
  (:import hiphip.benchmark.lng.JavaBaseline))

(load "type_impl")
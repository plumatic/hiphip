(ns hiphip.benchmark.long
  "Benchmarks for double arrays"
  (:use clojure.test)
  (:require
   [hiphip.long :as hiphip]
   [criterium.core :as bench]
   [clojure.pprint :as pprint])
  (:import hiphip.long_.JavaBaseline))

(load "type_impl")
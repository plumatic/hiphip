(ns hiphip.benchmark.float
  "Benchmarks for double arrays"
  (:use clojure.test)
  (:require
   [hiphip.float :as hiphip]
   [criterium.core :as bench]
   [clojure.pprint :as pprint])
  (:import hiphip.float_.JavaBaseline))

(load "type_impl")
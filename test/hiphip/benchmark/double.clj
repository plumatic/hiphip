(ns hiphip.benchmark.double
  "Benchmarks for double arrays"
  (:use clojure.test)
  (:require
   [hiphip.double :as hiphip]
   [criterium.core :as bench]
   [clojure.pprint :as pprint])
  (:import hiphip.double_.JavaBaseline))

(load "type_impl")
(ns hiphip.benchmark.int
  "Benchmarks for double arrays"
  (:use clojure.test)
  (:require
   [hiphip.int :as hiphip]
   [criterium.core :as bench]
   [clojure.pprint :as pprint])
  (:import hiphip.int_.JavaBaseline))

(load "type_impl")
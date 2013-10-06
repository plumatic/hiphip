(ns hiphip.double-test
  "Benchmarks for double arrays"
  (:require [hiphip.double :as hiphip])
  (:import hiphip.double_.Baseline)
  (:use hiphip.type_impl_test))

(use 'clojure.test 'hiphip.test-utils)
(require '[hiphip.impl.core :as impl])
(eval (read-string (load-type-impl-test)))

(ns hiphip.long-test
  "Benchmarks for double arrays"
  (:require [hiphip.long :as hiphip])
  (:import hiphip.long_.Baseline)
  (:use hiphip.type_impl_test))

(use 'clojure.test 'hiphip.test-utils)
(require '[hiphip.impl.core :as impl])
(eval (read-string (load-type-impl-test)))
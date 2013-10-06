(ns hiphip.int-test
  "Benchmarks for double arrays"
  (:require [hiphip.int :as hiphip])
  (:import hiphip.int_.Baseline)
  (:use hiphip.type_impl_test))

(use 'clojure.test 'hiphip.test-utils)
(require '[hiphip.impl.core :as impl])
(eval (read-string (load-type-impl-test)))
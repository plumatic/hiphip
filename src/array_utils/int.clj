(ns ^{:doc "Utilities for int[]"
      :author "EHF"}
  array-utils.int
  (:use array-utils.core)
  (:refer-clojure :exclude [amap areduce alength]))

(set! *warn-on-reflection* true)

;; # Int implementations

(def type-info
  {:sg `int
   :atype "[I"
   :constructor `int-array
   :min-value Integer/MIN_VALUE
   :max-value Integer/MAX_VALUE})

(load "type_impl")

(ns ^{:doc "Utilities for long[]"
      :author "EHF"}
  array-utils.long
  (:use array-utils.core)
  (:refer-clojure :exclude [amap areduce alength]))

(set! *warn-on-reflection* true)

;; # Long implementations

(def type-info
  {:sg `long
   :atype "[J"
   :constructor `long-array
   :min-value Long/MIN_VALUE
   :max-value Long/MAX_VALUE})

(load "type_impl")

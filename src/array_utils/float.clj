(ns ^{:doc "Utilities for float[]"
      :author "EHF"}
  array-utils.float
  (:use array-utils.core)
  (:refer-clojure :exclude [amap]))

(set! *warn-on-reflection* true)

;; # Float implementations

;; Please refer to `core.clj/abind-hint` for information on how the
;; bindings work.

(def type-info
  {:sg `float
   :atype "[F"
   :constructor `float-array
   :min-value Float/MIN_VALUE
   :max-value Float/MAX_VALUE})

(load "type_impl")

(ns ^{:doc "Utilities for double[]"
      :author "EHF"}
  array-utils.double
  (:use array-utils.core)
  (:refer-clojure :exclude [amap areduce]))

(set! *warn-on-reflection* true)

;; # Double implementations

;; Please refer to `core.clj/abind-hint` for information on how the
;; bindings work.

(def type-info
  {:sg `double
   :atype "[D"
   :constructor `double-array
   :min-value Double/MIN_VALUE
   :max-value Double/MAX_VALUE})

(load "type_impl")

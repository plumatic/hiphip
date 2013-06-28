(ns ^{:doc "Utilities for long[]"
      :author "EHF"}
  array-utils.long
  (:use array-utils.core)
  (:refer-clojure :exclude [amap areduce alength aget aset aclone]))

(set! *warn-on-reflection* true)

;; # Long implementations

(def type-info
  {:etype `long
   :atype "[J"
   :constructor `long-array
   :min-value Long/MIN_VALUE
   :max-value Long/MAX_VALUE})

(load "type_impl")

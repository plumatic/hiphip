(ns array-utils.float
  "Utilities for float arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone]))

(def type-info
  {:etype `float
   :atype "[F"
   :constructor `float-array
   :min-value `Float/MIN_VALUE
   :max-value `Float/MAX_VALUE})

(load "type_impl")

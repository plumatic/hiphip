(ns hiphip.long
  "Utilities for long arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.long_.JavaBaseline))

(def type-info
  {:etype `long
   :atype "[J"
   :constructor `long-array
   :min-value `Long/MIN_VALUE
   :max-value `Long/MAX_VALUE})

(load "type_impl")

(ns hiphip.float
  "Utilities for float arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.float_.Helpers))

(def +type+ 'float)

(load "type_impl")

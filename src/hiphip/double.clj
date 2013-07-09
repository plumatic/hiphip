(ns hiphip.double
  "Utilities for double arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.double_.Helpers))

(def +type+ 'double)

(load "type_impl")
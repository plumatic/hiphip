(ns hiphip.double
  "Utilities for double arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.double_.JavaBaseline))

(def +type+ 'double)

(load "type_impl")
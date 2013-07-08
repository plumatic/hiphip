(ns hiphip.int
  "Utilities for int arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.double_.JavaBaseline))

(def +type+ 'int)

(load "type_impl")

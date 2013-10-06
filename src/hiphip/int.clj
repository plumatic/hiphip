(ns hiphip.int
  "Utilities for int arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.int_.Helpers)
  (:use hiphip.type_impl))

(def ^:dynamic +type+ 'int)
(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])
(eval (read-string (load-type-impl)))

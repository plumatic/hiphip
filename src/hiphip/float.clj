(ns hiphip.float
  "Utilities for float arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.float_.Helpers)
  (:use hiphip.type_impl))

(def ^:dynamic +type+ 'float)
(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])
(eval (read-string (load-type-impl)))

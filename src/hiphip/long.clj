(ns hiphip.long
  "Utilities for long arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.long_.Helpers)
  (:use hiphip.type_impl))

(def ^:dynamic +type+ 'long)
(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])
(eval (read-string (load-type-impl)))

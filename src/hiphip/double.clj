(ns hiphip.double
  "Utilities for double arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.double_.Helpers)
  (:use hiphip.type_impl))

(def ^:dynamic +type+ 'double)
(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])
(eval (read-string (load-type-impl)))

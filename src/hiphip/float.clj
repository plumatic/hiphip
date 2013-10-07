(ns hiphip.float
  "Utilities for float arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:require [hiphip.impl.core :as impl])
  (:import hiphip.float_.Helpers))

(def +type+ 'float)

(load-string (impl/slurp-from-classpath "hiphip/type_impl.clj"))

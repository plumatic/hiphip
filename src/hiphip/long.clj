(ns hiphip.long
  "Utilities for long arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:require [hiphip.impl.core :as impl])
  (:import hiphip.long_.Helpers))

(def +type+ 'long)

(load-string (impl/slurp-from-classpath "hiphip/type_impl.clj"))

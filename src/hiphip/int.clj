(ns hiphip.int
  "Utilities for int arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:require [hiphip.impl.core :as impl])
  (:import hiphip.int_.Helpers))

(def +type+ 'int)

(load-string (impl/slurp-from-classpath "hiphip/type_impl.clj"))

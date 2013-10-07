(ns hiphip.double
  "Utilities for double arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:require [hiphip.impl.core :as impl])
  (:import hiphip.double_.Helpers))

(def +type+ 'double)

(load-string (impl/slurp-from-classpath "hiphip/type_impl.clj"))

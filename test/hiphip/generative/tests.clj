(ns hiphip.generative.tests
  (:use hiphip.generators)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [clojure.data.generators :as gen]
            [clojure.test.generative.runner :as runner]
            [hiphip.double :as d]
            [hiphip.long :as l]
            [hiphip.int :as i])
  (:refer-clojure :exclude [amap areduce alength aclone aset]))

;; This is a small test-bed for general, composite functions making
;; use of our array API. It should not be used to test the individual
;; implementations. Feel free to contribute your own!

;; ---
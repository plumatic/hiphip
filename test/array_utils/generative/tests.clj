(ns array-utils.generative.tests
  (:use array-utils.generators)
  (:require [clojure.test.generative :as test :refer (defspec)]
            [clojure.data.generators :as gen]
            [clojure.test.generative.runner :as runner]
            [array-utils.double :as d]
            [array-utils.long :as l]
            [array-utils.int :as i])
  (:refer-clojure :exclude [amap areduce alength aclone aset]))

;; This is a small test-bed for general, composite functions making
;; use of our array API. It should not be used to test the individual
;; implementations. Feel free to contribute your own!

;; ---
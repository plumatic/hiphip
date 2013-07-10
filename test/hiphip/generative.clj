(ns hiphip.generative
  "The main entry point for running the generative tests."
  (:require
   [clojure.test :as test]
   [clojure.test.generative.runner :as runner]))

(test/deftest ^:gen-test gen-test
  (runner/-main "test/hiphip/generative"))
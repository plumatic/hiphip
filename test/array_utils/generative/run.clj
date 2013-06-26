(ns array-utils.generative.run
  "The main entry point for running the generative tests."
  (:require [clojure.test.generative.runner :as runner]))

(defn -main []
  (runner/-main "test/array_utils/generative"))
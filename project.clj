(defproject prismatic/hiphip "0.1.0-SNAPSHOT"
  :description "hiphip (array!): elegant methods for doing fast math
                with arrays in Clojure."
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :url "https://github.com/Prismatic/hiphip"
  :java-source-paths ["java"]
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:plugins [[lein-marginalia "0.7.1"]]
                   :dependencies [[org.clojure/clojure "1.5.1"]
                                  [criterium "0.4.1"]
                                  [org.clojure/test.generative "0.4.0"]]}}
  :test-selectors {:fast #(not (or (:bench %) (:gen-test %)))
                   :gen-test :gen-test
                   :bench :bench}
  :warn-on-reflection true
  ;; Clear out devault JVM opts set by leiningen that trade startup time for
  ;; optimization, making Clojure array code run slow.
  ;; You probably want this in your project too.
  :jvm-opts ^:replace [])

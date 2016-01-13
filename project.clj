(defproject prismatic/hiphip "0.2.2-SNAPSHOT"
  :description "hiphip (array!): elegant methods for doing fast math
                with arrays in Clojure."
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :url "https://github.com/plumatic/hiphip"
  :java-source-paths ["java"]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :dev {:global-vars {*warn-on-reflection* true}
                   :plugins [[lein-marginalia "0.7.1"]]
                   :dependencies [[org.clojure/clojure "1.6.0"]
                                  [criterium "0.4.1"]
                                  [org.clojure/test.generative "0.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.5:dev,1.7"]}
  :test-selectors {:fast #(not (or (:bench %) (:gen-test %)))
                   :gen-test :gen-test
                   :bench :bench}
  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy" "clojars"]}
  ;; Clear out devault JVM opts set by leiningen that trade startup time for
  ;; optimization, making Clojure array code run slow.
  ;; You probably want this in your project too.
  :jvm-opts ^:replace [])

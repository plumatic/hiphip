(defproject array-utils "0.2.0-ALPHA"
  :description "Utility macros and functions to make working with
                primitive arrays a bit more bearable."
  :url "http://vinjeboy.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["java"]
  :aliases {"gen-test" ["run" "-m" "array-utils.generative.run"]
            "test" ["do" "test," "gen-test"]
            "bench" ["run" "-m" "array-utils.benchmark"]}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:plugins [[lein-marginalia "0.7.1"]]
                   :dependencies [;; for benchmarking
                                  [criterium "0.4.1"]
                                  ;; awesome
                                  [org.clojure/test.generative "0.4.0"]]}}
  :warn-on-reflection true
  ;; It turns out that Leiningen sets some JVM options that prevent full
  ;; optimization, so doing 'lein run' may not be good for benchmarking. Clear
  ;; out those options!
  :jvm-opts ^:replace [])

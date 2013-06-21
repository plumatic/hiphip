(ns ^{:doc "Benchmarking suite for array-utils"
      :author "EHF"}
  array-utils.benchmark 
  (:use clojure.test)
  (:require [array-utils.double :as d]
            [array-utils.long :as l]
            [array-utils.generators :as gen]
            [criterium.core :as bench]))

;; # Utils and setup

(defn ensure-directory [name]
  (let [f (clojure.java.io/file name)]
    (if-not (.isDirectory f)
      (if (.isFile f)
        (do (println "Please delete the file `benchmark` or supply a new name.")
            (System/exit 0))
        (.mkdir f))
      (println "The folder" name "exists. Neat! Moving on."))))

;; Very unsafe, but to hell with it!
(defn gen-fname []
  (let [now (java.util.Date.)]
    (clojure.string/replace (str "benchmarks/" now ".txt") #"\s|\d\d:\d\d:\d\d" "")))

;; ----------------------------------------

;; # Benchmarking suite. Go wild!

;; TODO: Add more generative benchmarking using data.generators

;; TODO: Port over solutions from Alioth?

(defn dot-product-double [ws xs]
  (d/asum [w ws x xs] (* w x)))

(defn dot-product-long [ws xs]
  (l/asum [w ws x xs] (* w x)))

(defn RQD-doubles
  [xs core-diameter]
  (let [ys-sum (d/asum [x xs] (if (< (* 2.0 core-diameter) x) x 0.0))]
    (* 100.0 (/ ys-sum (d/asum xs)))))

;; ----------------------------------------

(def line (apply str (repeat 72 "=")))

(defmacro run-benchmark [expr]
  `(do (println (str "Testing the expression: " (str ~expr)))
       (bench/quick-bench ~expr)
       (println line)))

(defmacro run-benchmarks [& exprs]
  `(doseq [expr# '~exprs]
     (run-benchmark expr#)))

(defn benchmarks
  "Simple functions should be quick-checked and large, complicated
  operations be benched." []
  (with-out-str 
    (run-benchmarks
     (dot-product-double (gen/darray) (gen/darray))
     (dot-product-long (gen/larray) (gen/larray))
     (RQD-doubles (gen/darray)))
    ;; (bench/bench (solve-world-hunger))
    ))

(defn -main [& {:keys [dest] :or {dest "benchmarks"}}]
  (ensure-directory dest)
  (println "Benchmarking. This might take a while.")
  (let [res (benchmarks)
        fname (gen-fname)]
    (spit fname res :append true)
    (println (str "Done. Results stored to " fname))))
;;; Benchmark code shared between array types.
;; Assumes the appropriate hiphip array type ns has been aliased as 'hiphip',
;; and the appropriate Java baseline class has been imported as 'JavaBaseline'

(set! *unchecked-math* true)

(def type-info hiphip/type-info
  #_{:etype `double
     :atype "[D"
     :constructor `double-array
     :min-value `Double/MIN_VALUE
     :max-value `Double/MAX_VALUE})

(defmacro benchmark
  "Declare a benchmark, a list [options form options form ...]"
  [& exprs]
  (assert (even? (count exprs)))
  `(delay
    (binding [bench/*final-gc-problem-threshold* 0.4]
      [~@(for [[options expr] (partition 2 exprs)]
           `{:form (quote ~expr)
             :options ~options
             :results (try (bench/quick-benchmark ~expr
                                             {} #_ {:samples 3
                                                    :target-execution-time 100000000
                                                    :warmup-jit-period 500000000})
                        (catch Exception e#
                          {:exception e#}))})])))

(defmacro gen-array* []
  `(defn ^{:tag ~(:atype type-info)} gen-array [size# offset#]
     (~(:constructor type-info)
      (take size# (map #(+ offset# %) (apply concat (repeat [-2 -1 0 1 2 3 4])))))))

(gen-array*)

(defmacro benchmarks*
  "Create a sequence of delays that run benchmarks. This uses delays so
  we can show some results before all benchmarks are complete.
  A macro so we can properly typehint the baselines.
  Intentionally capture symbols so benchmark forms are readable."
  [size]
  `(defn benchmarks []
     (let [~'xs (gen-array ~size 0)
           ~'ys (gen-array ~size 2)]
       [(benchmark
         {} (JavaBaseline/alength ~'xs)
         {:expected-slowness 1.1} (hiphip/alength ~'xs))
        (benchmark
         {} (JavaBaseline/aget ~'xs 5)
         {:expected-slowness 1.1} (hiphip/aget ~'xs 5))
        (benchmark
         {} (JavaBaseline/aset ~'xs 5 3)
         {:expected-slowness 1.1} (hiphip/aset ~'xs 5 3))
        (benchmark
         {} (JavaBaseline/ainc ~'xs 5 3)
         {:expected-slowness 1.1} (hiphip/ainc ~'xs 5 3))
        (benchmark
         {} (JavaBaseline/aclone ~'xs)
         {:expected-slowness 1.1} (hiphip/aclone ~'xs))
        (benchmark
         {} (JavaBaseline/dot_product ~'xs ~'ys)
         {:expected-slowness 1.1} (hiphip/areduce [~'x ~'xs ~'y ~'ys]
                                                  ~'ret (~(:etype type-info) 0)
                                                  (+ ~'ret (* ~'x ~'y)))
         {:expected-slowness 1.1} (hiphip/dot-product ~'xs ~'ys)
         {} (areduce ~'xs ~'i ~'ret (~(:etype type-info) 0)
                     (+ ~'ret (* (aget ~'xs ~'i)
                                 (aget ~'ys ~'i))))
         {} (reduce + (map * ~'xs ~'ys)))
        (benchmark
         {} (JavaBaseline/multiply_in_place_pointwise ~'xs ~'ys)
         {:expected-slowness 1.1} (hiphip/doarr [[~'i ~'x] ~'xs ~'y ~'ys]
                                                (hiphip/aset ~'xs ~'i (* ~'x ~'y)))
         {:expected-slowness 1.1} (hiphip/afill! [~'x ~'xs ~'y ~'ys] (* ~'x ~'y)))
        (benchmark
         {} (JavaBaseline/multiply_in_place_by_idx ~'xs)
         {:expected-slowness 1.1} (hiphip/afill! [[~'i ~'x] ~'xs] (* ~'x ~'i)))
        (benchmark
         {} (JavaBaseline/acopy_inc ~size ~'xs)
         {:expected-slowness 1.1} (hiphip/amake [~'i ~size] (inc (hiphip/aget ~'xs ~'i))))
        (benchmark
         {} (JavaBaseline/amap_inc ~'xs)
         {:expected-slowness 1.1} (hiphip/amap [~'x ~'xs] (inc ~'x))
         {} (amap ~'xs ~'i ~'ret (aset ~'ret ~'i (inc (aget ~'xs ~'i)))))
        (benchmark
         {} (JavaBaseline/amap_plus_idx ~'xs)
         {:expected-slowness 1.1} (hiphip/amap [[~'i ~'x] ~'xs] (+ ~'i ~'x)))
        (benchmark
         {} (JavaBaseline/asum ~'xs)
         {:expected-slowness 1.1} (hiphip/asum ~'xs)
         {} (areduce ~'xs ~'i ~'ret (~(:etype type-info) 0)
                     (+ ~'ret (aget ~'xs ~'i)))
         {} (reduce + ~'xs))
        (benchmark
         {} (JavaBaseline/asum_square ~'xs)
         {:expected-slowness 1.1} (hiphip/asum [~'x ~'xs] (* ~'x ~'x)))
        (benchmark
         {} (JavaBaseline/aproduct ~'xs)
         {:expected-slowness 1.1} (hiphip/aproduct ~'xs))
        (benchmark
         {} (JavaBaseline/amax ~'xs)
         ;; amax is inexplicably slower with *unchecked-math* on...
         {:expected-slowness 1.7} (hiphip/amax ~'xs))
        (benchmark
         {} (JavaBaseline/amin ~'xs)
         ;; amax is inexplicably slower with *unchecked-math* on...
         {:expected-slowness 1.7} (hiphip/amin ~'xs))
        (benchmark
         {} (JavaBaseline/amean ~'xs)
         {:expected-slowness 1.1} (hiphip/amean ~'xs))])))

(benchmarks* 10000)

(defn print-benchmark
  "Pretty-print a benchmark comparison."
  [bench-data]
  (let [baseline (first bench-data)]
    (pprint/print-table
     [:form :slowness :ms :variance]
     (for [benchmark bench-data]
       (into {:form (:form benchmark)}
             (if (get-in benchmark [:results :exception])
               {:slowness "exception"
                :ms "exception"
                :variance "exception"}
               {:slowness (format "%f"
                                  (/ (-> benchmark :results :mean first)
                                     (-> baseline :results :mean first)))
                :ms (->> benchmark :results :mean first (* 1e3) (format "%f"))
                :variance (->> benchmark :results :variance first
                               (* 1e3) (format "%f"))}))))))

(deftest benchmarks-test
  (testing "Benchmarks are appropriately fast."
    (doseq [benchmark (benchmarks)]
      (let [baseline (first @benchmark)]
        (doseq [result @benchmark]
          (when-let [expected-slowness
                     (get-in result [:options :expected-slowness])]
            (let [slowness (/ (-> result :results :mean first)
                              (-> baseline :results :mean first))]
              (is (< slowness expected-slowness)
                  (format "%s was too slow!"
                          (:form result)
                          slowness expected-slowness)))))))))

(defn -main
  []
  (println "Benchmarking. This might take a while.")
  (doseq [benchmark (benchmarks)]
    (print-benchmark @benchmark)))

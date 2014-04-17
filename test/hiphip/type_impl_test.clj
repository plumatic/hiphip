;;; Benchmark code shared between array types.
;; Assumes the appropriate hiphip array type ns has been aliased as 'hiphip',
;; and the appropriate Java baseline class has been imported as 'Baseline'

(use 'clojure.test 'hiphip.test-utils)
(require '[hiphip.impl.core :as impl])

(def +type+ hiphip/+type+)

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests for selection and partitioning

(defn partitioned? [s pivot]
  (let [[smaller more] ((juxt take-while drop-while) #(< % pivot) s)
        [eq more] ((juxt take-while drop-while) #(= % pivot) more)]
    (every? #(> % pivot) more)))

(defn selected? [s k]
  (let [[small big] (split-at k s)]
    (is (<= (apply max small) (apply min big)))))

(defn ascending? [s] (= (seq s) (sort s)))

(defn max-sorted? [s k]
  (let [[front back] (split-at (- (count s) k) s)]
    (and (every? #(<= % (first back)) front)
         (ascending? back))))

(defn min-sorted? [s k]
  (let [[front back] (split-at k s)]
    (and (every? #(>= % (last front)) back)
         (ascending? front))))

(defn de-index [arr indices]
  (is (= (count arr) (count indices)))
  (let [v (vec arr)]
    (map #(nth v %) indices)))

(defn into-arr [s]
  (let [v (vec s)]
    (hiphip/amake [i (count v)] (nth v i))))

(defn direct-partition-ops-tests [s k]
  (let [a (into-arr s)
        pivot (nth a k)]
    (is (= a (hiphip/apartition! a pivot)))
    (is (partitioned? a pivot)))
  (let [a (into-arr s)]
    (is (= a (hiphip/aselect! a k)))
    (is (selected? a k)))
  (let [a (into-arr s)]
    (is (= a (hiphip/asort! a)))
    (is (ascending? a)))
  (let [a (into-arr s)]
    (is (= a (hiphip/asort-max! a k)))
    (is (max-sorted? a k)))
  (let [a (into-arr s)]
    (is (= a (hiphip/asort-min! a k)))
    (is (min-sorted? a k))))

(defn index-partition-ops-tests [s k]
  (let [a (into-arr s)
        pivot (nth a k)]
    (is (= (map long a) s))
    (let [indices (hiphip.IndexArrays/make 0 (count s))]
      (is (partitioned? (de-index a (hiphip/apartition-indices! indices a pivot)) pivot)))
    (let [indices (hiphip.IndexArrays/make 0 (count s))]
      (is (selected? (de-index a (hiphip/aselect-indices! indices a k)) k)))
    (is (ascending? (de-index a (hiphip/asort-indices! a))))
    (is (max-sorted? (de-index a (hiphip/amax-indices a k)) k))
    (is (min-sorted? (de-index a (hiphip/amin-indices a k)) k))))

(deftest big-partition-and-sort-ops-test
  (let [n 1000
        r (java.util.Random. 1)]
    (doseq [[test-name t] {"direct" direct-partition-ops-tests
                           "indirect" index-partition-ops-tests}
            [seq-name s] {"zeros" (repeat n 0)
                          "asc" (range n)
                          "desc" (reverse (range n))
                          "rand" (repeatedly n #(.nextInt r 10000))
                          "rand-repeated" (repeatedly n #(.nextInt r 100))}
            k [1 10 50 100 500 900 950 990 999]]
      (testing (format "%s opts on %s with k=%s" test-name n k)
        (t s k)))))

(deftest simple-partition-and-sort-opts-test
  (is (= [1 1 2 2 2 3]
         (map long (hiphip/apartition! (into-arr [2 1 2 1 2 3]) 2))))
  (is (= [1 1 2 2]
         (map long (hiphip/aselect! (into-arr [1 2 2 1]) 2))))
  (is (= [1 2 3 4]
         (map long (hiphip/asort! (into-arr [4 2 3 1])))))
  (is (= [3 3 4]
         (take-last 3 (map long (hiphip/asort-max! (into-arr [-2 3 4 2 3 1 -1]) 3)))))
  (is (= [-2 -1 1]
         (take 3 (map long (hiphip/asort-min! (into-arr [-2 3 4 2 3 1 -1]) 3)))))

  (is (= [1 2 0]
         (seq (hiphip/apartition-indices! (hiphip.IndexArrays/make 0 3)
                                          (into-arr [3 1 2]) 2))))
  (is (= [1 0]
         (seq (hiphip/aselect-indices! (hiphip.IndexArrays/make 0 2) (into-arr [2 1]) 1))))
  (is (= [3 1 2 0]
         (seq (hiphip/asort-indices! (into-arr [4 2 3 1])))))
  (is (= [4 2 1]
         (take-last 3 (hiphip/amax-indices (into-arr [-2 5 4 2 3 1 -1]) 3))))
  (is (= [0 6 5]
         (take 3 (hiphip/amin-indices (into-arr [-2 3 4 2 3 1 -1]) 3)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Simple tests for typehinted hiphip.array fns, including :let and :range

(deftest simple-binding-macro-test
  (is (= [1 2 3] (map long (hiphip/amake [i 3] (inc i)))))
  (is (= 10 (hiphip/areduce [x (into-arr [1 2 3 4])] r 0 (+ r (long x)))))
  (is (= 26 (hiphip/areduce [:range [1 3]
                             x (into-arr [1 2 3 4])
                             :let [y (* x 2)]]
                            r 0 (+ r (long (* x y))))))
  (let [res (atom [])]
    (hiphip/doarr [:range [1 3]
                   [i x] (into-arr [1 2 3 4])
                   :let [y (+ i x)]]
                  (swap! res conj [i (long x) (long y)]))
    (is (= [[1 2 3] [2 3 5]] @res)))
  (is (= [3 5]
         (map long (hiphip/amap [:range [1 3]
                                 [i x] (into-arr [1 2 3 4])
                                 :let [y (+ i x)]]
                                y))))
  (let [a (into-arr [1 2 3 4])]
    (is (= a (hiphip/afill! [:range [1 3] x a x a :let [y (* x x)]]
                            (+ y 2))))
    (is (= [1 6 11 4] (map long a)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Benchmark/equality tests

(defn- select-slowness
  "If slowness is given as a type map, pull out the correct value"
  [slowness-and-exprs]
  (->> (partition 2 slowness-and-exprs)
       (mapcat (fn [[slowness expr]]
                 [(if (map? slowness) (get slowness (keyword (name +type+))) slowness)
                  expr]))))

(defmacro defbenchmarktype
  "Wrapper around defbenchmark for fns that take two arrays of the primitive type being tested,
   and allows expressing type-dependent slowness like:
   {:double slowness :float slowness :int slowness :long slowness}"
  [name expr & slowness-and-exprs]
  (assert (even? (count slowness-and-exprs)))
  `(defbenchmark ~name [~(impl/array-cast +type+ 'xs) ~(impl/array-cast +type+ 'ys)]
     ~expr
     ~@(select-slowness slowness-and-exprs)))

(defbenchmarktype aclone
  (Baseline/aclone xs)
  1.1 (hiphip/aclone xs))

(defbenchmarktype alength
  (Baseline/alength xs)
  1.1 (hiphip/alength xs)
  ;; failure cases for testing the tests
  ;; 1.2 (inc (hiphip/alength xs))
  ;; 1.2 (do (hiphip/aset xs 0 100) (hiphip/alength xs))
  ;; 0.3 (do (hiphip/aset ys 0 101) (hiphip/alength xs))
  )

(defbenchmarktype aget
  (Baseline/aget xs 0)
  1.1 (hiphip/aget xs 0))

(defbenchmarktype aset
  (Baseline/aset xs 0 42)
  1.1 (hiphip/aset xs 0 42))

(set! *unchecked-math* true)

(defbenchmarktype ainc
  (Baseline/ainc xs 0 1)
  1.1 (hiphip/ainc xs 0 1))

(defbenchmarktype amake
  (Baseline/acopy_inc (hiphip/alength xs) xs)
  {:double 1.1 :float 1.7 :long 1.9 :int 1.3}
  (hiphip/amake [i (hiphip/alength xs)] (inc (hiphip/aget xs i))))

;; Arrays should be correctly cast even if the RHS is somewhat complex.
(defbenchmarktype array-cast
  (let [m {:xs {:data [xs]}}]
    (let [xs-new (-> m (get-in [:xs :data]) first)]
      (hiphip/afill! [x xs-new] (+ x 2))))
  1.1 (let [m {:xs {:data [xs]}}]
        (hiphip/afill! [x (-> m (get-in [:xs :data]) first)] (+ x 2))))

;; helpers for areduce-and-dot-product

(defmacro hinted-hiphip-areduce [bind ret-sym init final]
  `(hiphip/areduce ~bind ~ret-sym ~(impl/value-cast +type+ init) ~final))

(defmacro hinted-clojure-areduce [arr-sym idx-sym ret-sym init final]
  `(areduce ~arr-sym ~idx-sym ~ret-sym ~(impl/value-cast +type+ init) ~final))

(set! *unchecked-math* false)

(defbenchmarktype areduce-and-dot-product-no-unchecked
  (Baseline/dot_product xs ys)

  {:double 1.1 :float 1.7 :long 30.0 :int 30.0}
  (hinted-hiphip-areduce [x xs y ys] ret 0 (+ ret (* x y)))

  {:double 1.1 :float 1.7 :long 30.0 :int 30.0}
  (hiphip/dot-product xs ys)

  nil (hinted-clojure-areduce xs i ret 0 (+ ret (* (aget xs i) (aget ys i))))
  nil (reduce + (map * xs ys)))

(set! *unchecked-math* true)

(defbenchmarktype areduce-and-dot-product
  (Baseline/dot_product xs ys)

  {:double 1.4 :float 1.8 :long 2.6 :int 2.9}
  (hinted-hiphip-areduce [x xs y ys] ret 0 (+ ret (* x y)))

  {:double 1.4 :float 1.8 :long 2.6 :int 2.9}
  (hiphip/dot-product xs ys)

  nil (hinted-clojure-areduce xs i ret 0 (+ ret (* (aget xs i) (aget ys i))))
  nil (reduce + (map * xs ys)))

(defbenchmarktype doarr-and-afill!
  (Baseline/multiply_in_place_pointwise xs ys)

  {:double 1.4 :float 3.3 :long 1.6 :int 2.4}
  (do (hiphip/doarr [[i x] xs y ys] (hiphip/aset xs i (* x y))) xs)

  {:double 1.4 :float 3.3 :long 1.6 :int 2.4}
  (hiphip/afill! [x xs y ys] (* x y)))

(defbenchmarktype doarr-and-afill-range!
  (Baseline/multiply_end_in_place_pointwise xs ys)

  {:double 1.5 :float 3.3 :long 1.6 :int 2.4}
  (do (hiphip/doarr [:range [(quot (alength xs) 2) (alength xs)]
                     [i x] xs
                     y ys]
                    (hiphip/aset xs i (* x y)))
      xs)

  {:double 1.5 :float 3.3 :long 1.6 :int 2.4}
  (hiphip/afill! [:range [(quot (alength xs) 2) (alength xs)]
                  x xs
                  y ys]
                 (* x y)))

;; slightly faster for double with unchecked off
(defbenchmarktype afill-with-index!
  (Baseline/multiply_in_place_by_idx xs)
  2.8 (hiphip/afill! [[i x] xs] (* x i)))

(defbenchmarktype amap
  (Baseline/amap_inc xs)
  {:double 1.1 :float 1.7 :long 1.1 :int 1.4}
  (hiphip/amap [x xs] (inc x))
  nil (amap xs i ret (aset ret i (inc (aget xs i)))))

(defbenchmarktype amap-range
  (Baseline/amap_end_inc xs)
  {:double 1.5 :float 2.2 :long 1.3 :int 1.6}
  (hiphip/amap [:range [(quot (alength xs) 2) (alength xs)] x xs] (inc x)))

(defbenchmarktype amap-with-index
  (Baseline/amap_plus_idx xs)
  {:double 1.1 :float 1.5 :long 1.1 :int 1.3}
  (hiphip/amap [[i x] xs] (+ i x)))

(defbenchmarktype asum
  (Baseline/asum xs)

  {:double 1.1 :float 1.1 :long 3.3 :int 2.4}
  (hiphip/asum xs)

  nil (hinted-clojure-areduce xs i ret 0 (+ ret (aget xs i)))
  nil (reduce + xs))

(defbenchmarktype asum-range
  (Baseline/asum_end xs)
  {:double 1.1 :float 1.1 :long 7.0 :int 2.6}
  (hiphip/asum [:range [(quot (alength xs) 2) (alength xs)] x xs] x))

(set! *unchecked-math* false)

(defbenchmarktype asum-op-no-unchecked
  (Baseline/asum_square xs)
  {:double 1.1 :float 1.6 :long 25.0 :int 20.0}
  (hiphip/asum [x xs] (* x x)))

(set! *unchecked-math* true)

(defbenchmarktype asum-op
  (Baseline/asum_square xs)
  {:double 1.1 :float 2.2 :long 2.3 :int 1.7}
  (hiphip/asum [x xs] (* x x)))

(defbenchmarktype aproduct
  (Baseline/aproduct xs)
  1.1 (hiphip/aproduct xs))

(defbenchmarktype amean
  (Baseline/amean xs)
  {:double 1.1 :float 1.1 :long 3.3 :int 3.3}
  (hiphip/amean xs))


(defmacro amax-clj
  "Maximum over an array."
  [xs]
  (let [rsym (gensym "r") xsym (gensym "x")]
    `(let [xs# ~xs]
       (hiphip/areduce [~xsym xs#]
                       ~rsym
                       (aget xs# 0)
                       ~(impl/value-cast +type+ `(if (> ~rsym ~xsym) ~rsym ~xsym))))))

;; these implicitly test max/min-index
(defbenchmarktype amax
  (Baseline/amax xs)
  1.1 (hiphip/amax xs)
  nil (amax-clj xs))

(defbenchmarktype amin
  (Baseline/amin xs)
  1.1 (hiphip/amin xs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Top-level benchmark/equality test runners

(defn gen-array [size phase]
  (hiphip/amake [i size] (nth [-2 3 0 -1 0 1 -1 2 3] (mod (+ i phase) 9))))

(let [me *ns*]
  (defn all-tests [size]
    (doseq [[n t] (ns-interns me)
            :when (.startsWith ^String (name n) "test-")]
      (t (gen-array size 0) (gen-array size 1))))

  (defn all-benches [size]
    (doseq [[n t] (ns-interns me)
            :when (.startsWith ^String (name n) "bench-")]
      (t (gen-array size 0) (gen-array size 1)))))

(deftest hiphip-type-test
  (all-tests 10000))

(deftest ^:bench hiphip-type-bench
  (all-benches 10000))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Test sorting ops, with no equality and differnet data

(set! *unchecked-math* false)

(defmacro deftestfasttype
  "Wrapper around deftestfast -- like defbenchmarktype, but does not declare a test."
  [name expr & slowness-and-exprs]
  (assert (even? (count slowness-and-exprs)))
  `(deftestfast ~name [~(impl/array-cast +type+ 'xs) ~(impl/array-cast +type+ 'ys)]
     ~expr
     ~@(select-slowness slowness-and-exprs)))

(deftestfasttype sort-ops
  (java.util.Arrays/sort xs)
  0.1 (hiphip/aselect! xs (quot (alength xs) 2))
  0.2 (hiphip/aselect! xs (quot (alength xs) 10))
  0.2 (hiphip/aselect! xs 1)
  0.03 (hiphip/amax xs)
  0.3 (hiphip/asort-max! xs (quot (alength xs) 10))
  6.0 (hiphip/asort-indices! xs)
  1.0 (hiphip/amax-indices xs (quot (alength xs) 10))
  0.6 (hiphip/amax-indices xs 5))

(deftest ^:bench sort-ops-bench
  (let [r (java.util.Random. 1)
        xs (hiphip/amake [_ 10000] (.nextInt r 1000000))]
    (sort-ops xs xs)))

(set! *warn-on-reflection* false)

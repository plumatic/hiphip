;;; Benchmark code shared between array types.
;; Assumes the appropriate hiphip array type ns has been aliased as 'hiphip',
;; and the appropriate Java baseline class has been imported as 'Baseline'

(use 'clojure.test 'hiphip.test-utils)
(require '[hiphip.impl.core :as impl])

(def +type+ hiphip/+type+)

(set! *warn-on-reflection* true)

;; TODO: some versions with and without unchecked math -- it actually makes many things slower.
;; (set! *unchecked-math* true)

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

(defn test-direct-partition-ops [s k]
  (let [a (into-arr s)
        pivot (nth a k)]
    (is (= a (hiphip/apartition a pivot)))
    (is (partitioned? a pivot)))
  (let [a (into-arr s)]
    (is (= a (hiphip/aselect a k)))
    (is (selected? a k)))
  (let [a (into-arr s)]
    (is (= a (hiphip/asort a)))
    (is (ascending? a)))
  (let [a (into-arr s)]
    (is (= a (hiphip/asort-max a k)))
    (is (max-sorted? a k)))
  (let [a (into-arr s)]
    (is (= a (hiphip/asort-min a k)))
    (is (min-sorted? a k))))

(defn test-index-partition-ops [s k]
  (let [a (into-arr s)
        pivot (nth a k)]
    (let [indices (hiphip.IndexArrays/make 0 (count s))]
      (is (partitioned? (de-index a (hiphip/apartition-indices indices a pivot)) pivot)))
    (let [indices (hiphip.IndexArrays/make 0 (count s))]
      (is (selected? (de-index a (hiphip/aselect-indices indices a k)) k)))
    (is (ascending? (de-index a (hiphip/asort-indices a))))
    (is (max-sorted? (de-index a (hiphip/amax-indices a k)) k))
    (is (min-sorted? (de-index a (hiphip/amin-indices a k)) k))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Miscellaneous extra tests

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Benchmark/equality tests

(defmacro defbenchmarktype
  "Wrapper around defbenchmark for fns that take two arrays of the primitive type being tested,
   and allows expressing type-dependent slowness like:
   {:double slowness :float slowness :int slowness :long slowness}"
  [name expr & slowness-and-exprs]
  (assert (even? (count slowness-and-exprs)))
  `(defbenchmark ~name [~(impl/array-cast +type+ 'xs) ~(impl/array-cast +type+ 'ys)]
     ~expr
     ~@(->> (partition 2 slowness-and-exprs)
            (mapcat (fn [[slowness expr]]
                      [(if (map? slowness) (get slowness (keyword (name +type+))) slowness)
                       expr])))))

(defbenchmarktype aclone
  (Baseline/aclone xs)
  1.1 (hiphip/aclone xs)

  ;; 1.1 (double-array (hiphip/alength xs))
  ;; 1.1 (make-array Double/TYPE (hiphip/alength xs))
  )

(defbenchmarktype alength
  (Baseline/alength xs)
  1.1 (hiphip/alength xs)
  ;; failure cases for testing the tests
  ;; 1.2 (inc (hiphip/alength xs))
  ;; 1.2 (do (aset xs 0 100.0) (hiphip/alength xs))
  ;; 0.3 (do (aset ys 0 101.0) (hiphip/alength xs))
  )

(defbenchmarktype aget
  (Baseline/aget xs 0)
  1.1 (hiphip/aget xs 0))

(defbenchmarktype aset
  (Baseline/aset xs 0 42)
  1.1 (hiphip/aset xs 0 42))

(defbenchmarktype ainc
  (Baseline/ainc xs 0 1)
  1.1 (hiphip/ainc xs 0 1))

(defmacro hinted-hiphip-areduce [bind ret-sym init final]
  `(hiphip/areduce ~bind ~ret-sym ~(impl/value-cast +type+ init) ~final))

(defmacro hinted-clojure-areduce [arr-sym idx-sym ret-sym init final]
  `(areduce ~arr-sym ~idx-sym ~ret-sym ~(impl/value-cast +type+ init) ~final))

(defbenchmarktype areduce-and-dot-product
  (Baseline/dot_product xs ys)
  1.1 (hinted-hiphip-areduce [x xs y ys] ret 0 (+ ret (* x y)))
  1.1 (hiphip/dot-product xs ys)
  nil (hinted-clojure-areduce xs i ret 0 (+ ret (* (aget xs i) (aget ys i))))
  nil (reduce + (map * xs ys)))

(defbenchmarktype doarr-and-afill!
  (Baseline/multiply_in_place_pointwise xs ys)
  1.1 (do (hiphip/doarr [[i x] xs y ys] (hiphip/aset xs i (* x y))) xs)
  1.1 (hiphip/afill! [x xs y ys] (* x y)))

(defbenchmarktype doarr-and-afill-range!
  (Baseline/multiply_end_in_place_pointwise xs ys)
  1.1 (do (hiphip/doarr [:range [(quot (alength xs) 2) (alength xs)]
                         [i x] xs
                         y ys]
                        (hiphip/aset xs i (* x y))) xs)
  1.1 (hiphip/afill! [:range [(quot (alength xs) 2) (alength xs)]
                      x xs
                      y ys]
                     (* x y)))

(defbenchmarktype afill!
  (Baseline/multiply_in_place_by_idx xs)
  1.1 (hiphip/afill! [[i x] xs] (* x i)))

(defbenchmarktype amake
  (Baseline/acopy_inc (hiphip/alength xs) xs)
  1.1 (hiphip/amake [i (hiphip/alength xs)] (inc (hiphip/aget xs i))))

(defbenchmarktype amap
  (Baseline/amap_inc xs)
  1.1 (hiphip/amap [x xs] (inc x))
  nil (amap xs i ret (aset ret i (inc (aget xs i)))))

(defbenchmarktype amap-range
  (Baseline/amap_end_inc xs)
  1.1 (hiphip/amap [:range [(quot (alength xs) 2) (alength xs)] x xs] (inc x)))

(defbenchmarktype amap-with-index
  (Baseline/amap_plus_idx xs)
  1.1 (hiphip/amap [[i x] xs] (+ i x)))

(defbenchmarktype asum
  (Baseline/asum xs)
  1.1 (hiphip/asum xs)
  nil (hinted-clojure-areduce xs i ret 0 (+ ret (aget xs i)))
  nil (reduce + xs))

(defbenchmarktype asum-range
  (Baseline/asum_end xs)
  1.1 (hiphip/asum [:range [(quot (alength xs) 2) (alength xs)] x xs] x))

(defbenchmarktype asum-op
  (Baseline/asum_square xs)
  1.1 (hiphip/asum [x xs] (* x x)))

(defbenchmarktype aproduct
  (Baseline/aproduct xs)
  1.1 (hiphip/aproduct xs))

(comment
  (defmacro amax
    "Maximum over an array."
    [xs]
    `(areduce [x# ~xs] m# ~(:min-value type-info) (~(:etype type-info) (if (> m# x#) m# x#))))

  (defmacro amax2
    "Maximum over an array."
    [xs]
    `(aget ~xs (Baseline/maxIndex ~xs)))

  (defmacro amax3
    "Maximum over an array."
    [xs]
    `(let [xs# ~xs
           len# (alength xs#)]
       (loop [i# 1 m# (aget xs# 0)]
         (if (== i# len#)
           m#
           (let [v# (aget xs# i#)]
             (if (> v# m#)
               (recur (unchecked-inc-int i#) v#)
               (recur (unchecked-inc-int i#) m#))))))))

(defbenchmarktype amax-index
  (hiphip/amax-index xs)
  ;; 1.7 (hiphip/amax-index2 xs)
  ;; ;; 1.7 (hiphip/amax2 xs)
  ;; ;; 1.7 (hiphip/amax3 xs)
  )

(defbenchmarktype amax
  (Baseline/amax xs)
  ;; amax is inexplicably slower with *unchecked-math* on...
  1.7 (hiphip/amax xs)
  ;; 1.7 (hiphip/amax2 xs)
  ;; 1.7 (hiphip/amax3 xs)
  )

(defbenchmarktype amin
  (Baseline/amin xs)
  ;; amax is inexplicably slower with *unchecked-math* on...
  1.7 (hiphip/amin xs))

(defbenchmarktype amean
  (Baseline/amean xs)
  1.1 (hiphip/amean xs))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Top-level benchmark/equality test runners

(defn gen-array [size phase]
  (hiphip/amake [i size] (nth [-2 3 0 -1 0 1 -1 2 3] (mod (+ i phase) 9))))

(let [me *ns*]
  (defn all-tests [size]
    (doseq [[n t] (ns-interns me)
            :when (.startsWith ^String (name n) "test-")]
      (testing (name n)
        (t (gen-array size 0) (gen-array size 1)))))

  (defn all-benches [size]
    (doseq [[n t] (ns-interns me)
            :when (.startsWith ^String (name n) "bench-")]
      (testing (name n)
        (t (gen-array size 0) (gen-array size 1))))))

(set! *warn-on-reflection* false)
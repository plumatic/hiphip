;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)
(require '[hiphip.core :as core])

(definline aclone
  "aclone that doesn't require type hinting"
  [xs]
  `(clojure.core/aclone ~(with-meta xs {:tag (:atype type-info)})))

(definline alength
  "alength that doesn't require type hinting"
  [xs]
  `(clojure.core/alength ~(with-meta xs {:tag (:atype type-info)})))

(definline aget
  "aset that doesn't require type hinting"
  [xs idx]
  `(clojure.core/aget ~(with-meta xs {:tag (:atype type-info)}) ~(core/intcast idx)))

(definline aset
  "aset that doesn't require type hinting"
  [xs idx val]
  `(clojure.core/aset ~(with-meta xs {:tag (:atype type-info)}) ~(core/intcast idx)
                      (~(:etype type-info) ~val)))

(definline ainc
  "Increment the value of xs at idx by val"
  [xs idx val]
  `(let [idx# ~idx]
     (aset ~xs idx# (+ (~(:etype type-info) ~val) (aget ~xs idx#)))))

(defmacro areduce
  "Areduce, with hiphip-style array bindings.

  Note: The type of the accumulator will have the same semantics as those of a
  variable in a loop."
  [bindings ret init form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (core/parse-bindings type-info bindings)]
    `(let ~initial-bindings
       (loop [~index-sym ~start-sym ~ret ~init]
         (if (< ~index-sym ~stop-sym)
           (recur (unchecked-inc-int ~index-sym)
                  (let ~value-bindings ~form))
           ~ret)))))

(defmacro doarr
  "Like doseq, but with hiphip-style array bindings."
  [bindings & body]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (core/parse-bindings type-info bindings)]
    `(let ~initial-bindings
       (core/dotimes-int [~index-sym ~start-sym ~stop-sym]
                         (let ~value-bindings ~@body)))))

(defmacro amake
  "Make a new array of length len and fill it with values computed by expr."
  [[idx len] expr]
  `(let [len# ~(core/intcast len)
         a# (~(:constructor type-info) len#)]
     (core/dotimes-int [~idx len#] (aset a# ~idx ~expr))
     a#))

(defmacro amap
  "Like for, but with hiphip-style array bindings.  Builds a new array from
   values produced by form at each step, with length equal to the range of
   the iteration."
  [bindings form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (core/parse-bindings type-info bindings)
        fsym (first initial-bindings)
        out-sym (core/typed-gensym "out" (:atype type-info))]
    `(let ~(into initial-bindings [out-sym `(~(:constructor type-info) (- ~stop-sym ~start-sym))])
       (core/dotimes-int [~index-sym ~start-sym ~stop-sym]
                         (let ~value-bindings (aset ~out-sym (unchecked-add ~start-sym ~index-sym) ~form)))
       ~out-sym)))

(defmacro afill!
  "Like `amap`, but writes the output of form to the first bound array and returns it."
  [bindings form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (core/parse-bindings type-info bindings)]
    `(let ~initial-bindings
       (core/dotimes-int [~index-sym ~start-sym ~stop-sym]
                         (let ~value-bindings (aset ~(first initial-bindings) ~index-sym ~form)))
       ~(first initial-bindings))))

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(asum [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings sum# (~(:etype type-info) 0) (+ sum# ~form))))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings prod# (~(:etype type-info) 1) (* prod# ~form))))

(defmacro amean
  "Mean over an array."
  [xs]
  `(let [xs# ~xs]
     (/ (asum xs#) (alength xs#))))

(defmacro dot-product
  "Dot product of two arrays."
  [xs ys]
  `(let [xs# ~xs ys# ~ys]
     (asum [x# xs# y# ys#] (* x# y#))))

(definline amax-index
  "Maximum over an array.
   Uses Java for now for maximum efficiency.
   See benchmarks for our current best performance in pure Clojure."
  [xs]
  `(JavaBaseline/maxIndex ~xs))

(definline amax
  "Maximum over an array."
  [xs]
  `(let [xs# ~xs] (aget xs# (amax-index xs#))))

(definline amin-index
  "Maximum over an array.
   Uses Java for now for maximum efficiency.
   See benchmarks for our current best performance in pure Clojure."
  [xs]
  `(JavaBaseline/minIndex ~xs))

(definline amin
  "Maximum over an array."
  [xs]
  `(let [xs# ~xs] (aget xs# (amin-index xs#))))

(defmacro apartition
  "Mutate array xs in range [start stop) so that elements less than pivot come first,
   followed by elements equal to pivot, followed by elements greater than pivot.
   Returns 1 + the smallest index pointing at an element > pivot after the partitioning."
  ([xs pivot] `(let [xs# ~xs] (apartition xs# 0 (alength xs#) ~pivot)))
  ([xs start stop pivot]
     `(doto ~xs (JavaBaseline/partition ~start ~stop ~pivot))))

(defmacro aselect
  ([xs k] `(let [xs# ~xs] (aselect xs# 0 (alength xs#) ~k)))
  ([xs start stop k]
     `(doto ~xs (JavaBaseline/select ~start ~stop ~k))))

(defmacro asort
  ([xs]
     `(doto ~(with-meta xs {:tag (:atype type-info)})
        java.util.Arrays/sort))
  ([xs start stop]
     `(doto ~(with-meta xs {:tag (:atype type-info)})
        (java.util.Arrays/sort ~start ~stop))))

(defn asort-max
  "Rearrange xs so that the last k elements are the top k in ascending order.
   Faster than sorting the whole array."
  [xs ^long k]
  (let [len (alength xs)]
    (aselect xs (- len k))
    (asort xs (- len k) len)
    xs))

(defn asort-min
  "Rearrange xs so that the first k elements are the min k in ascending order.
   Faster than sorting the whole array."
  [xs ^long k]
  (aselect xs k)
  (asort xs 0 k)
  xs)



(defmacro apartition-indices
  "Like partition, but  mutate an array of indices into arr rather than
   modifying array directly."
  ([indices xs pivot]
     `(let [indices# ~indices]
        (apartition-indices indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~pivot)))
  ([indices xs start stop pivot]
     `(doto ~indices (JavaBaseline/partitionIndices ~xs ~start ~stop ~pivot))))

(defmacro aselect-indices
  ([indices xs k]
     `(let [indices# ~indices]
        (aselect-indices indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~k)))
  ([indices xs start stop k]
     `(doto ~indices (JavaBaseline/selectIndices ~xs ~start ~stop ~k))))

(defmacro asort-indices
  ([xs]
     `(let [xs# ~xs] (asort-indices xs# 0 (alength xs#))))
  ([indices xs]
     `(let [indices# ~indices]
        (asort-indices indices# ~xs 0 (hiphip.IndexArrays/length indices#))))
  ([xs start stop]
     `(doto (hiphip.IndexArrays/make ~start ~stop)
        (asort-indices ~xs)))
  ([indices xs start stop]
     `(doto ~indices (JavaBaseline/sortIndices ~xs ~start ~stop))))

;; TODO: fix weird API?
(defn ^ints amax-indices
  "Return an array of indices where the last k elements point at the max
   k elements of xs in ascending order (and the remaining elements point
   at the remaining elements of xs, in no particular order.)"
  [xs ^long k]
  (let [len (alength xs)]
    (doto (hiphip.IndexArrays/make 0 len)
      (aselect-indices xs (- len k))
      (asort-indices xs (- len k) len))))

(defn ^ints amin-indices
  "Return an array of indices where the first k elements point at the min
   k elements of xs in ascending order (and the remaining elements point
   at the remaining elements of xs, in no particular order.)"
  [xs ^long k]
  (doto (hiphip.IndexArrays/make 0 (alength xs))
    (aselect-indices xs k)
    (asort-indices xs 0 k)))

(set! *warn-on-reflection* false)

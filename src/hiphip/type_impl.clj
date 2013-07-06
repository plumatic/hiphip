;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(set! *warn-on-reflection* true)
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
  ([xs pivot]
     `(let [xs# ~xs] (apartition xs# 0 (alength xs#) pivot)))
  ([xs start stop pivot]
     `(JavaBaseline/partition ~xs ~start ~stop ~pivot)))

(defmacro apartition-indices
  "Like partition, but (create and) mutate an array of indices into arr rather than
   modifying array directly."
  ([xs pivot]
     `(let [xs# ~xs] (apartition-indices xs# 0 (alength xs#) pivot)))
  ([indices xs pivot]
     `(let [xs# ~xs] (apartition-indices indices# xs# 0 (alength xs#) pivot)))
  ([xs start stop pivot]
     `(let [xs# ~xs
            indices# (hiphip.IndexArrays/make ~start ~stop)]
        (apartition-indices indices# xs# 0 (alength indices#) pivot)))
  ([indices xs start stop pivot]
     `(JavaBaseline/partition ~indices ~xs ~start ~stop ~pivot)))


(set! *warn-on-reflection* false)

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

(defmacro amax
  "Maximum over an array."
  [xs]
  `(areduce [x# ~xs] m# ~(:min-value type-info) (~(:etype type-info) (if (> m# x#) m# x#))))

(defmacro amin
  "Minimum over an array."
  [xs]
  `(areduce [x# ~xs] m# ~(:max-value type-info) (~(:etype type-info) (if (< m# x#) m# x#))))

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

(set! *warn-on-reflection* false)

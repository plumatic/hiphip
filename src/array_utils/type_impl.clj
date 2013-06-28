;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(set! *warn-on-reflection* true)
(require '[array-utils.core :as core])


(defmacro typecast
  "Internal: cast a value to the array's type."
  [v]
  `(~(:etype type-info) ~v))

(defmacro alength
  "alength that doesn't require type hinting"
  [xs]
  `(clojure.core/alength ~(with-meta xs {:tag (:atype type-info)})))

(defmacro aget
  "aset that doesn't require type hinting"
  [xs idx]
  `(clojure.core/aget ~(with-meta xs {:tag (:atype type-info)}) ~(core/intcast idx)))

(defmacro aset
  "aset that doesn't require type hinting"
  [xs idx val]
  `(clojure.core/aset ~(with-meta xs {:tag (:atype type-info)}) ~(core/intcast idx)
                      ~val))

(defmacro aclone
  "aclone that doesn't require type hinting"
  [xs]
  `(clojure.core/aclone ~(with-meta xs {:tag (:atype type-info)})))

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
       (loop [~index-sym ~start-sym]
         (when (< ~index-sym ~stop-sym)
           (let ~value-bindings ~@body)
           (recur (unchecked-inc-int ~index-sym)))))))

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
       (loop [~index-sym ~start-sym]
         (when (< ~index-sym ~stop-sym)
           (let ~value-bindings (aset ~out-sym (unchecked-add ~start-sym ~index-sym) ~form))
           (recur (unchecked-inc-int ~index-sym))))
       ~out-sym)))

(defmacro afill!
  "Like `amap`, but writes the output of form to the first bound array and returns it."
  [bindings form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (core/parse-bindings type-info bindings)]
    `(let ~initial-bindings
       (loop [~index-sym ~start-sym]
         (when (< ~index-sym ~stop-sym)
           (let ~value-bindings (aset ~(first initial-bindings) ~index-sym ~form))
           (recur (unchecked-inc-int ~index-sym))))
       ~(first initial-bindings))))

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(asum [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings sum# (typecast 0) (+ sum# ~form))))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings prod# (typecast 1) (* prod# ~form))))

(defmacro amax
  "Maximum over an array."
  [xs]
  `(areduce [x# ~xs] m# ~(:min-value type-info) (if (> m# x#) m# x#)))

(defmacro amin
  "Minimum over an array."
  [xs]
  `(areduce [x# ~xs] m# ~(:max-value type-info) (if (< m# x#) m# x#)))

(defn amean
  "Mean over an array."
  [xs]
  (/ (asum xs) (alength xs)))

(defn dot-product
  "Dot product of two arrays."
  [xs ys]
  (asum [x xs y ys] (* x y)))

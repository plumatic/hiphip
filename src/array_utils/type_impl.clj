;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

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
  `(clojure.core/aget ~(with-meta xs {:tag (:atype type-info)}) ~(intcast idx)))

(defmacro aset
  "aset that doesn't require type hinting"
  [xs idx val]
  `(clojure.core/aset ~(with-meta xs {:tag (:atype type-info)}) ~(intcast idx)
                      (~(:etype type-info) ~val)))

(defmacro aclone
  "aclone that doesn't require type hinting"
  [xs]
  `(clojure.core/aclone ~(with-meta xs {:tag (:atype type-info)})))

(defmacro areduce
  "Areduce, with for-like bindings.

  Note: The type of the accumulator will have the same semantics as those of a
  variable in a loop."
  [bindings ret init form]
  `(areduce-hint ~type-info ~bindings ~ret ~init ~form))

(defmacro amap
  "Builds a new array from evaluating the body at each step. Uses for-like
  bindings."
  [bindings form]
  `(amap-hint ~type-info ~bindings ~form))

(defmacro doarr
  "Like doseq, but for arrays. Uses for-like bindings."
  ([bindings & body]
     `(doarr-hint ~type-info ~bindings ~@body)))

(defmacro afill!
  "Like `amap`, but with destructive mapping on the first array in the
  bindings."
  [bindings form]
  `(afill-hint! ~type-info ~bindings ~form))

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

(defn amax
  "Maximum over an array."
  [xs]
  (areduce [x xs] m (:min-value type-info) (max m x)))

(defn amin
  "Minimum over an array."
  [xs]
  (areduce [x xs] m (:max-value type-info) (min m x)))

(defn amean
  "Mean over an array."
  [xs]
  (/ (asum xs) (alength xs)))

(defn dot-product
  "Dot product of two arrays."
  [xs ys]
  (asum [x xs y ys] (* x y)))

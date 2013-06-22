;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(defmacro typecast
  "Internal: cast a value to the array's type."
  [v]
  `(~(:sg type-info) ~v))

(defmacro alength
  "Mostly internal: alength that doesn't require reflection"
  [xs]
  `(clojure.core/alength (~(:pl type-info) ~xs)))

(defmacro areduce
  "Areduce, with for-like bindings.

  Note: The type of the accumulator will have the same semantics as those of a
  variable in a loop."
  [bindings ret init body]
  `(areduce-hint ~type-info ~bindings ~ret ~init ~body))

(defmacro amap
  "Builds a new array from evaluating the body at each step. Uses for-like
  bindings."
  [bindings body]
  `(amap-hint ~type-info ~bindings ~body))

(defmacro doarr
  "Like doseq, but for arrays. Uses for-like bindings."
  ([bindings & body]
     `(doarr-hint ~type-info ~bindings ~@body)))

(defmacro afill!
  "Like `amap`, but with destructive mapping on the first array in the
  bindings."
  [bindings body]
  `(afill-hint! ~type-info ~bindings ~body))

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(asum [a# ~array] a#))
  ([bindings body]
     `(areduce ~bindings sum# (typecast 0) (+ sum# ~body))))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings body]
     `(areduce ~bindings prod# (typecast 1) (* prod# ~body))))

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

;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(defmacro typecast
  "Internal: cast a value to the array's type."
  [v]
  `(~(:sg type-info) ~v))

(defmacro reduce-with
  "Reduce, with for-like bindings.

  Note: The type of the accumulator will have the same semantics as those of a
  variable in a loop."
  [bindings ret init body]
  `(reduce-with-hint ~type-info ~bindings ~ret ~init ~body))

(defmacro amap
  "Builds a new array from evaluating the body at each step. Uses for-like
  bindings."
  [bindings body]
  `(amap-hint ~type-info ~bindings ~body))

(defmacro doarr
  "Like doseq, but for arrays. Uses for-like bindings."
  ([bindings & body]
     `(doarr-hint ~type-info ~bindings ~@body)))

(defmacro doarr-bounded
  "Like `doarr`, but on a subset of the array"
  [[start stop] bindings & body]
  `(doarr-bound-hint ~type-info [~start ~stop] ~bindings ~@body))

(defmacro afill!
  "Like `amap`, but with destructive mapping on the first array in the
  bindings."
  [bindings body]
  `(afill-hint! ~type-info ~bindings ~body))

(defmacro afill-bounded!
  "Like `afill!`, but over a subset of the array."
  [[start stop] bindings body]
  `(afill-bound-hint! ~type-info [~start ~stop] ~bindings ~body))

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(asum [a# ~array] a#))
  ([bindings body]
     `(reduce-with ~bindings sum# (typecast 0) (+ sum# ~body))))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings body]
     `(reduce-with ~bindings prod# (typecast 1) (* prod# ~body))))

(defn amax [xs] (reduce-with [x xs] m (:min-value type-info) (max m x)))

(defn amin [xs] (reduce-with [x xs] m (:max-value type-info) (min m x)))

(defn amean [xs] (/ (asum xs) (alength xs)))

(defn dot-product [xs ys] (asum [x xs y ys] (* x y)))

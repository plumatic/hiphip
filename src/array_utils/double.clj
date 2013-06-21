(ns ^{:doc "Utilities for double[]"
      :author "EHF"}
  array-utils.double
  (:use array-utils.core)
  (:refer-clojure :exclude [amap])) 

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)
;; enable on pain of (REPL) death
(set! *print-length* 15) 

;; # Double implementations

;; Please refer to `core.clj/abind-hint` for information on how the
;; bindings work.

(defmacro reduce-with
  "Standard reduce, but with for-each bindings (see `abind-hint`).
  Applies `f` to the accumulator and the body at each step. `unit`
  should satisfy `(f x unit) => x`."
  [[f unit] bindings & body]
  `(reduce-with-hint [double doubles] [~f ~unit] ~bindings ~@body))

(defmacro amap 
  "Builds a new array from evaluating the body at each step. See
  `abind-hint` for more information about the bindings."
  [bindings & body]
  `(amap-hint [double doubles] ~bindings ~@body))

(defmacro doarr 
  "Like doseq, but for arrays. See `abind-hint` on bindings."
  ([bindings & body]
     `(doarr-hint [double doubles] ~bindings ~@body)))

(defmacro doarr-bounded
  "Like `doarr`, but on a subset of the array"
  [[start stop] bindings & body]
  `(doarr-bound-hint [double doubles] [~start ~stop] ~bindings ~@body))

(defmacro afill! 
  "Like `amap`, but with destructive mapping on the first array in the
  bindings (for sanity reasons)."
  [bindings & body]
  `(afill-hint! [double doubles aset-double] ~bindings ~@body))

(defmacro afill-bounded! 
  "Like `afill!`, but over a subset of the array."
  [[start stop] bindings & body]
  `(afill-bound-hint! [double doubles aset-double] [~start ~stop] ~bindings ~@body))

(defn afilter
  "Like filter, but for arrays. Given a unit element, inserts it when
  an element does not satisfy the predicate, improving performance in
  a pinch."
  ([pred array]
     (let [acc (transient [])]
       (doarr [a array]
              (when (pred a)
                (conj! acc a)))
       (double-array (persistent! acc))))
  ([pred array unit]
     (amap [a array] (if (pred a) a unit))))

(defn afilter2
  "Like `afilter`, but uses `java.util.ArrayList`."
  ([pred array]
     (let [^java.util.ArrayList acc (java.util.ArrayList.)]
       (doarr [a array]
              (when (pred a)
                (.add acc a)))
       (double-array acc)))
  ([pred array unit]
     (amap [a array] (if (pred a) a unit))))

(defn afilter!
  "Destructive `afilter`."
  [pred array unit]
  (afill! [a array] (if (pred a) a unit)))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each bindings and
a body statement (see `abind-hint`)."
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings & body]
     `(reduce-with [* (double 1)]
        ~bindings ~@body)))

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each bindings and
a body statement (see `abind-hint`)."
  ([array]
     `(asum [a# ~array] a#))
  ([bindings & body]
     `(reduce-with [+ (double 0)]
        ~bindings ~@body)))

(defn collect
  "Like `areduce`, but with regular `reduce` arguments."
  [f unit xs]
  (reduce-with [f unit]
    [x xs] x))

(defn amax [xs] (collect max Double/MIN_VALUE xs))

(defn amin [xs] (collect min Double/MAX_VALUE xs))

(defn amean [xs] (/ (asum xs) (alength ^doubles xs)))
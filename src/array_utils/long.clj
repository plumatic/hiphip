(ns ^{:doc "Utilities for long[]"
      :author "EHF"}
  array-utils.long
  (:use array-utils.core)
  (:refer-clojure :exclude [amap])  ) 

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)
;; enable on pain of (REPL) death
(set! *print-length* 15) 

;; # Long implementations

;; Please refer to `core.clj/abind-hint` for information on how the
;; bindings work. Based on the double implementation. See
;; `doubles.clj` for information on each function in this namespace.

(defmacro reduce-with
  [[f unit] bindings & body]
  `(reduce-with-hint [long longs] [~f ~unit] ~bindings ~@body))

(defmacro amap [bindings & body]
  `(amap-hint [long longs] ~bindings ~@body))

(defmacro doarr [bindings & body]
  `(doarr-hint [long longs] ~bindings ~@body))

(defmacro doarr-bounded
  [[start stop] bindings & body]
  `(doarr-bound-hint [long longs] [~start ~stop] ~bindings ~@body))

(defmacro afill! [bindings & body]
  `(afill-hint! [long longs aset-long] ~bindings ~@body))

(defmacro afill-bounded! 
  [[start stop] bindings & body]
  `(afill-bound-hint! [long longs aset-long] [~start ~stop] ~bindings ~@body))

(defn afilter
  ([pred array]
     (let [acc (transient [])]
       (doarr [a array]
              (when (pred a)
                (conj! acc a)))
       (long-array (persistent! acc))))
  ([pred array unit]
     (amap [a array] (if (pred a) a unit))))

(defn afilter2
  ([pred array]
     (let [^java.util.ArrayList acc (java.util.ArrayList.)]
       (doarr [a array]
              (when (pred a)
                (.add acc a)))
       (long-array acc)))
  ([pred array unit]
     (amap [a array] (if (pred a) a unit))))

(defn afilter!
  [pred array unit]
  (afill! [a array] (if (pred a) a unit)))

(defmacro aproduct
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings & body]
     `(reduce-with [* (long 1)]
        ~bindings ~@body)))

(defmacro asum
  ([array]
     `(asum [a# ~array] a#))
  ([bindings & body]
     `(reduce-with [+ (long 0)]
        ~bindings ~@body)))

(defn collect
  [f unit xs]
  (reduce-with [f unit]
    [x xs] x))

(defn amax [xs] (collect max Long/MIN_VALUE xs))

(defn amin [xs] (collect min Long/MAX_VALUE xs))

(defn amean [xs] (/ (asum xs) (alength ^longs xs)))
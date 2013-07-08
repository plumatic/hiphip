(ns hiphip.array
  "Macros for iteration on generic arrays.  This namespace provides
   versions that can iterate over arbitrary mixtures of array types,
   but the arrays must be appropriately type hinted for good
   performance.  The specific array type namespaces (double/float/...)
   offer versions of these macros that do not require type hints, but
   only work on arrays of the corresponding type.

   All these macros use binding forms that look like:

   [[i x] xs
    y ys ...]

   This binds i to the index and x and y to the ith element of xs and
   ys, respectively. Note that unlike for/doseq, iteration over
   multiple arrays is parallel rather than nested.

   You can include index variables wherever and whenever you want, so
   you can do:

   [x xs
    y ys ...]

   or:

   [[i1 x] xs
    [i2 y] ys
    [i3 z] zs ...]

   but i1, i2, and i3 will have the same value.

   You can also include a range as a first element of the binding:

   [:range [0 10]
    [i x] xs]

   and the operation will only be applied over that range. The default
   range is from 0 to the length of the first array in the binding."
  (:refer-clojure :exclude [make-array amap areduce])
  (:require [hiphip.impl.core :as impl]))

(defmacro make-array
  "Like Clojure's make-array, but type must be a compile-time literal,
   correctly type-hints the output array, and primitives can be specified
   like 'double' in addition to Double/TYPE"
  [type len]
  (if-let [type-info (impl/primitive-type-info type)]
    `(~(:constructor type-info) ~len)
    (impl/array-cast type `(make-array ~type ~len))))

(defmacro amake
  "Make a new array of length len and element type type and fill it
  with values computed by expr."
  [type [idx len] expr]
  `(let [len# ~(impl/intcast len)
         a# (make-array ~type len#)]
     (impl/dotimes-int [~idx len#] (aset a# ~idx ~(impl/value-cast type expr)))
     a#))

(defmacro areduce
  "Areduce, with hiphip-style array bindings.

  Note: The type of the accumulator will have the same semantics as
  those of a variable in a loop."
  [bindings ret init form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (impl/parse-bindings bindings)]
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
        (impl/parse-bindings bindings)]
    `(let ~initial-bindings
       (impl/dotimes-int [~index-sym ~start-sym ~stop-sym]
                         (let ~value-bindings ~@body)))))

(defmacro amap
  "Like for, but with hiphip-style array bindings.  Builds a new array
   with element-type type from values produced by form at each step,
   with length equal to the range of the iteration."
  [type bindings form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (impl/parse-bindings bindings)
        fsym (first initial-bindings)
        out-sym (gensym "out")]
    `(let ~(into initial-bindings [out-sym `(make-array ~type (- ~stop-sym ~start-sym))])
       (impl/dotimes-int [~index-sym ~start-sym ~stop-sym]
                         (let ~value-bindings
                           (aset ~out-sym (unchecked-add ~start-sym ~index-sym)
                                 ~(impl/value-cast type form))))
       ~out-sym)))

(defmacro afill!
  "Like `amap`, but writes the output of form to the first bound array
  and returns it."
  [type bindings form]
  (let [{:keys [index-sym start-sym stop-sym initial-bindings value-bindings]}
        (impl/parse-bindings bindings)]
    `(let ~initial-bindings
       (impl/dotimes-int [~index-sym ~start-sym ~stop-sym]
                         (let ~value-bindings
                           (aset ~(first initial-bindings) ~index-sym
                                 ~(impl/value-cast type form))))
       ~(first initial-bindings))))

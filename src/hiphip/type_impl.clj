;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(def ^:private saved-warn-on-reflection *warn-on-reflection*)
(def ^:private saved-unchecked-math *unchecked-math*)
(set! *warn-on-reflection* true)
(set! *unchecked-math* true)
(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Type hinted versions of clojure.core fns, plus ainc

(definline aclone
  "aclone that doesn't require type hinting."
  [xs]
  `(clojure.core/aclone ~(impl/array-cast +type+ xs)))

(definline alength
  "alength that doesn't require type hinting"
  [xs]
  `(clojure.core/alength ~(impl/array-cast +type+ xs)))

(definline aget
  "aget that doesn't require type hinting"
  [xs idx]
  `(clojure.core/aget ~(impl/array-cast +type+ xs) ~(impl/intcast idx)))

(definline aset
  "aset that doesn't require type hinting"
  [xs idx val]
  `(clojure.core/aset ~(impl/array-cast +type+ xs) ~(impl/intcast idx)
                      ~(impl/value-cast +type+ val)))

(definline ainc
  "Increment the value of xs at idx by val"
  [xs idx val]
  `(let [idx# ~idx]
     (aset ~xs idx# (+ ~(impl/value-cast +type+ val) (aget ~xs idx#)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Type hinted versions of hiphip.array functions

(defmacro amake
  "Make a new array of length len and fill it with values computed by expr.

   ;; array of random values between 0 10
   (amake [_ 10e3] (rand-int 10))

   ;; array of squares
   (amake [i 100] (* i i))
  "
  [[idx len] expr]
  `(array/amake ~+type+ [~idx ~len] ~expr))

(defmacro areduce
  "`areduce`, with hiphip-style array bindings (please see the
  `hiphip.array` docstring). Note: The type of the accumulator will
  have the same semantics as those of a variable in a loop.

   ;; Sum a really, really large array without overflow
   (areduce [x xs] ret 0
     (+' ret x))

   ;; Frequencies of different elements
   (areduce [x xs] ret {}
     (assoc ret x (inc (get ret x 0))))

   ;; Return all non-composite numbers
   (areduce [x (asort xs)] ncomp []
     (if (some zero? (map #(mod x %) ncomp))
       ncomp
       (conj ncomp x)))
  "
  [bindings ret init form]
  `(array/areduce ~(impl/hint-bindings +type+ bindings) ~ret ~init ~form))

(defmacro doarr
  "Like doseq, but with hiphip-style array bindings (please see the
  `hiphip.array` docstring).

   ;; Array to ArrayList
   (let [alist (java.util.ArrayList.)]
     (doarr [x xs] (.add alist x))
     alist)

   ;; Print the fifty first elements
   (doarr [x xs :range [0 50]]
     (println x))
  "
  [bindings & body]
  `(array/doarr ~(impl/hint-bindings +type+ bindings) ~@body))

(defmacro amap
  "Like for, but with hiphip-style array bindings (please see the
   `hiphip.array` docstring). Builds a new array from values produced
   by form at each step, with length equal to the range of the
   iteration.

   ;; Square roots array
   (amap [x xs] (Math/sqrt x))

   ;; Take the max of two arrays.
   (amap [x xs y ys] (max x y))

   ;; Create an array from the first ten elements, all incremented
   (amap [x xs :range [0 10]] (inc x))
  "
  [bindings form]
  `(array/amap ~+type+ ~(impl/hint-bindings +type+ bindings) ~form))

(defmacro afill!
  "Like `amap`, but writes the output of form to the first bound array
   and returns it.

   ;; Relace values that are too large
   (afill! [x xs :let [limit 500]]
     (if (>= x limit) 42, x))

   ;; Cube an array!
   (afill! [x xs] (* x x x))

   ;; Zero the first 20 elements
   (afill! [x xs :range [0 20]] 0)
  "
  [bindings form]
  `(array/afill! ~+type+ ~(impl/hint-bindings +type+ bindings) ~form))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; More 'mathy' functions for the main numeric array types

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each
   bindings (please see the `hiphip.array` docstring) and a body
   expression.

   ;; Basic usage
   (asum xs)

   ;; Sum of the square of each element
   (asum [x xs] (* x x))

   ;; Compute a standard deviation
   (let [mean (amean xs)]
     (/ (asum [x xs] (Math/pow (- x mean) 2))
        (alength xs)))
  "
  ([array]
     `(asum [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings sum# ~(impl/value-cast +type+ 0) (+ sum# ~form))))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each
   bindings (please see the `hiphip.array` docstring) and a body
   expression.

   ;; Net probability of an array of probabilities
   (aproduct x)
  "
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings prod# ~(impl/value-cast +type+ 1) (* prod# ~form))))

(defmacro amean
  "Mean over an array."
  [xs]
  `(let [xs# ~xs]
     (/ (double (asum xs#)) (alength xs#))))

(defmacro dot-product
  "Dot product of two arrays."
  [xs ys]
  `(let [xs# ~xs ys# ~ys]
     (asum [x# xs# y# ys#] (* x# y#))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selecting minimal/maximal elements and sorting

(definline amax-index
  "Maximum over an array.

   Uses Java for now for maximum efficiency. See benchmarks for our
   current best performance in pure Clojure."
  [xs]
  `(Helpers/maxIndex ~xs))

(definline amax
  "Maximum over an array."
  [xs]
  `(let [xs# ~xs] (aget xs# (amax-index xs#))))

(definline amin-index
  "Minimum over an array.

   Uses Java for now for maximum efficiency. See benchmarks for our
   current best performance in pure Clojure."
  [xs]
  `(Helpers/minIndex ~xs))

(definline amin
  "Minimum over an array."
  [xs]
  `(let [xs# ~xs] (aget xs# (amin-index xs#))))

(defmacro apartition!
  "Mutate array xs in range [start stop) so that elements less than pivot come first,
   followed by elements equal to pivot, followed by elements greater
   than pivot. Returns 1 + the smallest index pointing at an element >
   pivot after the partitioning."
  ([xs pivot] `(let [xs# ~xs] (apartition! xs# 0 (alength xs#) ~pivot)))
  ([xs start stop pivot]
     `(doto ~xs (Helpers/partition ~start ~stop ~pivot))))

(defmacro aselect!
  "Rearranges xs such that the smallest k elements come first,
  followed by all greater elements."
  ([xs k] `(let [xs# ~xs] (aselect! xs# 0 (alength xs#) ~k)))
  ([xs start stop k]
     `(doto ~xs (Helpers/select ~start ~stop ~k))))

(defmacro asort!
  "Sorts an array in-place."
  ([xs]
     `(doto ~(impl/array-cast +type+ xs)
        java.util.Arrays/sort))
  ([xs start stop]
     `(doto ~(impl/array-cast +type+ xs)
        (java.util.Arrays/sort ~start ~stop))))

(defn asort-max!
  "Rearrange xs so that the last k elements are the top k in ascending order.
   Faster than sorting the whole array."
  [xs ^long k]
  (let [len (alength xs)]
    (aselect! xs (- len k))
    (asort! xs (- len k) len)
    xs))

(defn asort-min!
  "Rearrange xs so that the first k elements are the min k in ascending order.
   Faster than sorting the whole array."
  [xs ^long k]
  (aselect! xs k)
  (asort! xs 0 k)
  xs)

(defmacro apartition-indices!
  "Like apartition!, but mutate an array of indices instead."
  ([indices xs pivot]
     `(let [indices# ~indices]
        (apartition-indices! indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~pivot)))
  ([indices xs start stop pivot]
     `(doto ~indices (Helpers/partitionIndices ~xs ~start ~stop ~pivot))))

(defmacro aselect-indices!
  "Like aselect!, but mutates an array of indices instead."
  ([xs k]
     `(let [xs# ~xs] (aselect-indices! xs# 0 (alength xs#) ~k)))
  ([indices xs k]
     `(let [indices# ~indices]
        (aselect-indices! indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~k)))
  ([xs start stop k]
     `(doto (hiphip.IndexArrays/make ~start ~stop)
        (aselect-indices! ~xs ~k)))
  ([indices xs start stop k]
     `(doto ~indices (Helpers/selectIndices ~xs ~start ~stop ~k))))

(defmacro asort-indices!
  "Like asort!, but mutates an array of indices instead."
  ([xs]
     `(let [xs# ~xs] (asort-indices! xs# 0 (alength xs#))))
  ([indices xs]
     `(let [indices# ~indices]
        (asort-indices! indices# ~xs 0 (hiphip.IndexArrays/length indices#))))
  ([xs start stop]
     `(doto (hiphip.IndexArrays/make ~start ~stop)
        (asort-indices! ~xs)))
  ([indices xs start stop]
     `(doto ~indices (Helpers/sortIndices ~xs ~start ~stop))))

(defn ^ints amax-indices
  "Return an array of indices where the last k elements point at the
   max k elements of xs in ascending order (and the remaining elements
   point at the remaining elements of xs, in no particular order.)"
  [xs ^long k]
  (let [len (alength xs)]
    (doto (hiphip.IndexArrays/make 0 len)
      (aselect-indices! xs (- len k))
      (asort-indices! xs (- len k) len))))

(defn ^ints amin-indices
  "Return an array of indices where the first k elements point at the
   min k elements of xs in ascending order (and the remaining elements
   point at the remaining elements of xs, in no particular order.)"
  [xs ^long k]
  (doto (hiphip.IndexArrays/make 0 (alength xs))
    (aselect-indices! xs k)
    (asort-indices! xs 0 k)))

(set! *warn-on-reflection* saved-warn-on-reflection)
(set! *unchecked-math* saved-unchecked-math)

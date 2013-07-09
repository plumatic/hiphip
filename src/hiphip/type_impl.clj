;; As a hack to avoid writing macro-macros, this file defines the
;; per-type macros, and is loaded in each type's namespace.

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)
(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])

;; For documentation purposes only.
(def bindings
  "Array bindings come in pairs like this:

  [[i x] xs
   y ys
   ...]

  This binds `i` to the index and `x` and `y` to the ith element of xs
  and ys respectively. The index variable is optional. Also, do note
  that unlike for/doseq, iteration over multiple arrays is parallel
  rather than nested.

  The bindings can specify a range. The operations will then only be
  applied over this range. The default range is from 0 to the length
  of the first array in the bindings.

  [x xs :range [0 10]]

  Lastly, the bindings support a let statement. It casts the var(s) to
  the type of the array. Do note that destructuring syntax is not
  supported. Neither is shadowing any of the array bindings, which
  will throw an IllegalArgumentException.

  [x xs :let [alpha 5]
  "
  

  and bind 
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Type hinted versions of clojure.core fns, plus ainc

(definline aclone
  "aclone that doesn't require type hinting"
  [xs]
  `(clojure.core/aclone ~(impl/array-cast +type+ xs)))

(definline alength
  "alength that doesn't require type hinting"
  [xs]
  `(clojure.core/alength ~(impl/array-cast +type+ xs)))

(definline aget
  "aset that doesn't require type hinting"
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Type hinted versions of hiphip.array functions

(defmacro amake
  "Make a new array of length len and fill it with values computed by expr."
  [[idx len] expr]
  `(array/amake ~+type+ [~idx ~len] ~expr))

(defmacro areduce
  "Areduce, with hiphip-style array bindings.

  Note: The type of the accumulator will have the same semantics as those of a
  variable in a loop."
  [bindings ret init form]
  `(array/areduce ~(impl/hint-bindings +type+ bindings) ~ret ~init ~form))

(defmacro doarr
  "Like doseq, but with hiphip-style array bindings."
  [bindings & body]
  `(array/doarr ~(impl/hint-bindings +type+ bindings) ~@body))

(defmacro amap
  "Like for, but with hiphip-style array bindings.  Builds a new array from
   values produced by form at each step, with length equal to the range of
   the iteration."
  [bindings form]
  `(array/amap ~+type+ ~(impl/hint-bindings +type+ bindings) ~form))

(defmacro afill!
  "Like `amap`, but writes the output of form to the first bound array and returns it."
  [bindings form]
  `(array/afill! ~+type+ ~(impl/hint-bindings +type+ bindings) ~form))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; More 'mathy' functions for the main numeric array types.

(defmacro asum
  "Like `(apply + xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(asum [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings sum# ~(impl/value-cast +type+ 0) (+ sum# ~form))))

(defmacro aproduct
  "Like `(apply * xs)`, but for arrays. Supports for-each bindings and a body
  expression."
  ([array]
     `(aproduct [a# ~array] a#))
  ([bindings form]
     `(areduce ~bindings prod# ~(impl/value-cast +type+ 1) (* prod# ~form))))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Selecting minimal/maximal elements and sorting

(definline amax-index
  "Maximum over an array.
   Uses Java for now for maximum efficiency.
   See benchmarks for our current best performance in pure Clojure."
  [xs]
  `(Helpers/maxIndex ~xs))

(definline amax
  "Maximum over an array."
  [xs]
  `(let [xs# ~xs] (aget xs# (amax-index xs#))))

(definline amin-index
  "Maximum over an array.
   Uses Java for now for maximum efficiency.
   See benchmarks for our current best performance in pure Clojure."
  [xs]
  `(Helpers/minIndex ~xs))

(definline amin
  "Maximum over an array."
  [xs]
  `(let [xs# ~xs] (aget xs# (amin-index xs#))))

(defmacro apartition
  "Mutate array xs in range [start stop) so that elements less than pivot come first,
   followed by elements equal to pivot, followed by elements greater than pivot.
   Returns 1 + the smallest index pointing at an element > pivot after the partitioning."
  ([xs pivot] `(let [xs# ~xs] (apartition xs# 0 (alength xs#) ~pivot)))
  ([xs start stop pivot]
     `(doto ~xs (Helpers/partition ~start ~stop ~pivot))))

(defmacro aselect
  ([xs k] `(let [xs# ~xs] (aselect xs# 0 (alength xs#) ~k)))
  ([xs start stop k]
     `(doto ~xs (Helpers/select ~start ~stop ~k))))

(defmacro asort
  ([xs]
     `(doto ~(impl/array-cast +type+ xs)
        java.util.Arrays/sort))
  ([xs start stop]
     `(doto ~(impl/array-cast +type+ xs)
        (java.util.Arrays/sort ~start ~stop))))

(defn asort-max
  "Rearrange xs so that the last k elements are the top k in ascending order.
   Faster than sorting the whole array."
  [xs ^long k]
  (let [len (alength xs)]
    (aselect xs (- len k))
    (asort xs (- len k) len)
    xs))

(defn asort-min
  "Rearrange xs so that the first k elements are the min k in ascending order.
   Faster than sorting the whole array."
  [xs ^long k]
  (aselect xs k)
  (asort xs 0 k)
  xs)

(defmacro apartition-indices
  "Like partition, but  mutate an array of indices into arr rather than
   modifying array directly."
  ([indices xs pivot]
     `(let [indices# ~indices]
        (apartition-indices indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~pivot)))
  ([indices xs start stop pivot]
     `(doto ~indices (Helpers/partitionIndices ~xs ~start ~stop ~pivot))))

(defmacro aselect-indices
  ([indices xs k]
     `(let [indices# ~indices]
        (aselect-indices indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~k)))
  ([indices xs start stop k]
     `(doto ~indices (Helpers/selectIndices ~xs ~start ~stop ~k))))

(defmacro asort-indices
  ([xs]
     `(let [xs# ~xs] (asort-indices xs# 0 (alength xs#))))
  ([indices xs]
     `(let [indices# ~indices]
        (asort-indices indices# ~xs 0 (hiphip.IndexArrays/length indices#))))
  ([xs start stop]
     `(doto (hiphip.IndexArrays/make ~start ~stop)
        (asort-indices ~xs)))
  ([indices xs start stop]
     `(doto ~indices (Helpers/sortIndices ~xs ~start ~stop))))

(defn ^ints amax-indices
  "Return an array of indices where the last k elements point at the max
   k elements of xs in ascending order (and the remaining elements point
   at the remaining elements of xs, in no particular order.)"
  [xs ^long k]
  (let [len (alength xs)]
    (doto (hiphip.IndexArrays/make 0 len)
      (aselect-indices xs (- len k))
      (asort-indices xs (- len k) len))))

(defn ^ints amin-indices
  "Return an array of indices where the first k elements point at the min
   k elements of xs in ascending order (and the remaining elements point
   at the remaining elements of xs, in no particular order.)"
  [xs ^long k]
  (doto (hiphip.IndexArrays/make 0 (alength xs))
    (aselect-indices xs k)
    (asort-indices xs 0 k)))

(set! *warn-on-reflection* false)

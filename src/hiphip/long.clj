(ns hiphip.long
  "Utilities for long arrays"
  (:refer-clojure :exclude [amap areduce alength aget aset aclone])
  (:import hiphip.long_.Helpers))
(def ^:dynamic +type+ 'long)

;; Generate from the REPL with: (spit "some-output.txt" (pr-str (slurp "src/hiphip/type_impl.clj")))
(eval (read-string
";; As a hack to avoid writing macro-macros, this file defines the\r\n;; per-type macros, and is loaded in each type's namespace.\r\n(do \r\n(set! *warn-on-reflection* true)\r\n(set! *unchecked-math* true)\r\n(require '[hiphip.impl.core :as impl] '[hiphip.array :as array])\r\n\r\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\r\n;; Type hinted versions of clojure.core fns, plus ainc\r\n\r\n(definline aclone\r\n  \"aclone that doesn't require type hinting.\"\r\n  [xs]\r\n  `(clojure.core/aclone ~(impl/array-cast +type+ xs)))\r\n\r\n(definline alength\r\n  \"alength that doesn't require type hinting\"\r\n  [xs]\r\n  `(clojure.core/alength ~(impl/array-cast +type+ xs)))\r\n\r\n(definline aget\r\n  \"aset that doesn't require type hinting\"\r\n  [xs idx]\r\n  `(clojure.core/aget ~(impl/array-cast +type+ xs) ~(impl/intcast idx)))\r\n\r\n(definline aset\r\n  \"aset that doesn't require type hinting\"\r\n  [xs idx val]\r\n  `(clojure.core/aset ~(impl/array-cast +type+ xs) ~(impl/intcast idx)\r\n                      ~(impl/value-cast +type+ val)))\r\n\r\n(definline ainc\r\n  \"Increment the value of xs at idx by val\"\r\n  [xs idx val]\r\n  `(let [idx# ~idx]\r\n     (aset ~xs idx# (+ ~(impl/value-cast +type+ val) (aget ~xs idx#)))))\r\n\r\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\r\n;; Type hinted versions of hiphip.array functions\r\n\r\n(defmacro amake\r\n  \"Make a new array of length len and fill it with values computed by expr.\r\n\r\n   ;; array of random values between 0 10\r\n   (amake [_ 10e3] (rand-int 10))\r\n\r\n   ;; array of squares\r\n   (amake [i 100] (* i i))\r\n  \"\r\n  [[idx len] expr]\r\n  `(array/amake ~+type+ [~idx ~len] ~expr))\r\n\r\n(defmacro areduce\r\n  \"`areduce`, with hiphip-style array bindings (please see the\r\n  `hiphip.array` docstring). Note: The type of the accumulator will\r\n  have the same semantics as those of a variable in a loop.\r\n\r\n   ;; Sum a really, really large array without overflow\r\n   (areduce [x xs] ret 0\r\n     (+' ret x))\r\n\r\n   ;; Frequencies of different elements\r\n   (areduce [x xs] ret {}\r\n     (assoc ret x (inc (get ret x 0))))\r\n\r\n   ;; Return all non-composite numbers\r\n   (areduce [x (asort xs)] ncomp []\r\n     (if (some zero? (map #(mod x %) ncomp))\r\n       ncomp\r\n       (conj ncomp x)))\r\n  \"\r\n  [bindings ret init form]\r\n  `(array/areduce ~(impl/hint-bindings +type+ bindings) ~ret ~init ~form))\r\n\r\n(defmacro doarr\r\n  \"Like doseq, but with hiphip-style array bindings (please see the\r\n  `hiphip.array` docstring).\r\n\r\n   ;; Array to ArrayList\r\n   (let [alist (java.util.ArrayList.)]\r\n     (doarr [x xs] (.add alist x))\r\n     alist)\r\n\r\n   ;; Print the fifty first elements\r\n   (doarr [x xs :range [0 50]]\r\n     (println x))\r\n  \"\r\n  [bindings & body]\r\n  `(array/doarr ~(impl/hint-bindings +type+ bindings) ~@body))\r\n\r\n(defmacro amap\r\n  \"Like for, but with hiphip-style array bindings (please see the\r\n   `hiphip.array` docstring). Builds a new array from values produced\r\n   by form at each step, with length equal to the range of the\r\n   iteration.\r\n\r\n   ;; Square roots array\r\n   (amap [x xs] (Math/sqrt x))\r\n\r\n   ;; Take the max of two arrays.\r\n   (amap [x xs y ys] (max x y))\r\n\r\n   ;; Create an array from the first ten elements, all incremented\r\n   (amap [x xs :range [0 10]] (inc x))\r\n  \"\r\n  [bindings form]\r\n  `(array/amap ~+type+ ~(impl/hint-bindings +type+ bindings) ~form))\r\n\r\n(defmacro afill!\r\n  \"Like `amap`, but writes the output of form to the first bound array\r\n   and returns it.\r\n\r\n   ;; Relace values that are too large\r\n   (afill! [x xs :let [limit 500]]\r\n     (if (>= x limit) 42, x))\r\n\r\n   ;; Cube an array!\r\n   (afill! [x xs] (* x x x))\r\n\r\n   ;; Zero the first 20 elements\r\n   (afill! [x xs :range [0 20]] 0)\r\n  \"\r\n  [bindings form]\r\n  `(array/afill! ~+type+ ~(impl/hint-bindings +type+ bindings) ~form))\r\n\r\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\r\n;; More 'mathy' functions for the main numeric array types\r\n\r\n(defmacro asum\r\n  \"Like `(apply + xs)`, but for arrays. Supports for-each\r\n   bindings (please see the `hiphip.array` docstring) and a body\r\n   expression.\r\n\r\n   ;; Basic usage\r\n   (asum xs)\r\n\r\n   ;; Sum of the square of each element\r\n   (asum [x xs] (* x x))\r\n\r\n   ;; Compute a standard deviation\r\n   (let [mean (amean xs)]\r\n     (/ (asum [x xs] (Math/pow (- x mean) 2))\r\n        (alength xs)))\r\n  \"\r\n  ([array]\r\n     `(asum [a# ~array] a#))\r\n  ([bindings form]\r\n     `(areduce ~bindings sum# ~(impl/value-cast +type+ 0) (+ sum# ~form))))\r\n\r\n(defmacro aproduct\r\n  \"Like `(apply * xs)`, but for arrays. Supports for-each\r\n   bindings (please see the `hiphip.array` docstring) and a body\r\n   expression.\r\n\r\n   ;; Net probability of an array of probabilities\r\n   (aproduct x)\r\n  \"\r\n  ([array]\r\n     `(aproduct [a# ~array] a#))\r\n  ([bindings form]\r\n     `(areduce ~bindings prod# ~(impl/value-cast +type+ 1) (* prod# ~form))))\r\n\r\n(defmacro amean\r\n  \"Mean over an array.\"\r\n  [xs]\r\n  `(let [xs# ~xs]\r\n     (/ (double (asum xs#)) (alength xs#))))\r\n\r\n(defmacro dot-product\r\n  \"Dot product of two arrays.\"\r\n  [xs ys]\r\n  `(let [xs# ~xs ys# ~ys]\r\n     (asum [x# xs# y# ys#] (* x# y#))))\r\n\r\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\r\n;; Selecting minimal/maximal elements and sorting\r\n\r\n(definline amax-index\r\n  \"Maximum over an array.\r\n\r\n   Uses Java for now for maximum efficiency. See benchmarks for our\r\n   current best performance in pure Clojure.\"\r\n  [xs]\r\n  `(Helpers/maxIndex ~xs))\r\n\r\n(definline amax\r\n  \"Maximum over an array.\"\r\n  [xs]\r\n  `(let [xs# ~xs] (aget xs# (amax-index xs#))))\r\n\r\n(definline amin-index\r\n  \"Minimum over an array.\r\n\r\n   Uses Java for now for maximum efficiency. See benchmarks for our\r\n   current best performance in pure Clojure.\"\r\n  [xs]\r\n  `(Helpers/minIndex ~xs))\r\n\r\n(definline amin\r\n  \"Minimum over an array.\"\r\n  [xs]\r\n  `(let [xs# ~xs] (aget xs# (amin-index xs#))))\r\n\r\n(defmacro apartition!\r\n  \"Mutate array xs in range [start stop) so that elements less than pivot come first,\r\n   followed by elements equal to pivot, followed by elements greater\r\n   than pivot. Returns 1 + the smallest index pointing at an element >\r\n   pivot after the partitioning.\"\r\n  ([xs pivot] `(let [xs# ~xs] (apartition! xs# 0 (alength xs#) ~pivot)))\r\n  ([xs start stop pivot]\r\n     `(doto ~xs (Helpers/partition ~start ~stop ~pivot))))\r\n\r\n(defmacro aselect!\r\n  \"Rearranges xs such that the smallest k elements come first,\r\n  followed by all greater elements.\"\r\n  ([xs k] `(let [xs# ~xs] (aselect! xs# 0 (alength xs#) ~k)))\r\n  ([xs start stop k]\r\n     `(doto ~xs (Helpers/select ~start ~stop ~k))))\r\n\r\n(defmacro asort!\r\n  \"Sorts an array in-place.\"\r\n  ([xs]\r\n     `(doto ~(impl/array-cast +type+ xs)\r\n        java.util.Arrays/sort))\r\n  ([xs start stop]\r\n     `(doto ~(impl/array-cast +type+ xs)\r\n        (java.util.Arrays/sort ~start ~stop))))\r\n\r\n(defn asort-max!\r\n  \"Rearrange xs so that the last k elements are the top k in ascending order.\r\n   Faster than sorting the whole array.\"\r\n  [xs ^long k]\r\n  (let [len (alength xs)]\r\n    (aselect! xs (- len k))\r\n    (asort! xs (- len k) len)\r\n    xs))\r\n\r\n(defn asort-min!\r\n  \"Rearrange xs so that the first k elements are the min k in ascending order.\r\n   Faster than sorting the whole array.\"\r\n  [xs ^long k]\r\n  (aselect! xs k)\r\n  (asort! xs 0 k)\r\n  xs)\r\n\r\n(defmacro apartition-indices!\r\n  \"Like apartition!, but mutate an array of indices instead.\"\r\n  ([indices xs pivot]\r\n     `(let [indices# ~indices]\r\n        (apartition-indices! indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~pivot)))\r\n  ([indices xs start stop pivot]\r\n     `(doto ~indices (Helpers/partitionIndices ~xs ~start ~stop ~pivot))))\r\n\r\n(defmacro aselect-indices!\r\n  \"Like aselect!, but mutates an array of indices instead.\"\r\n  ([indices xs k]\r\n     `(let [indices# ~indices]\r\n        (aselect-indices! indices# ~xs 0 (hiphip.IndexArrays/length indices#) ~k)))\r\n  ([indices xs start stop k]\r\n     `(doto ~indices (Helpers/selectIndices ~xs ~start ~stop ~k))))\r\n\r\n(defmacro asort-indices!\r\n  \"Like asort!, but mutates an array of indices instead.\"\r\n  ([xs]\r\n     `(let [xs# ~xs] (asort-indices! xs# 0 (alength xs#))))\r\n  ([indices xs]\r\n     `(let [indices# ~indices]\r\n        (asort-indices! indices# ~xs 0 (hiphip.IndexArrays/length indices#))))\r\n  ([xs start stop]\r\n     `(doto (hiphip.IndexArrays/make ~start ~stop)\r\n        (asort-indices! ~xs)))\r\n  ([indices xs start stop]\r\n     `(doto ~indices (Helpers/sortIndices ~xs ~start ~stop))))\r\n\r\n(defn ^ints amax-indices\r\n  \"Return an array of indices where the last k elements point at the\r\n   max k elements of xs in ascending order (and the remaining elements\r\n   point at the remaining elements of xs, in no particular order.)\"\r\n  [xs ^long k]\r\n  (let [len (alength xs)]\r\n    (doto (hiphip.IndexArrays/make 0 len)\r\n      (aselect-indices! xs (- len k))\r\n      (asort-indices! xs (- len k) len))))\r\n\r\n(defn ^ints amin-indices\r\n  \"Return an array of indices where the first k elements point at the\r\n   min k elements of xs in ascending order (and the remaining elements\r\n   point at the remaining elements of xs, in no particular order.)\"\r\n  [xs ^long k]\r\n  (doto (hiphip.IndexArrays/make 0 (alength xs))\r\n    (aselect-indices! xs k)\r\n    (asort-indices! xs 0 k)))\r\n\r\n(set! *warn-on-reflection* false)\r\n)"
))
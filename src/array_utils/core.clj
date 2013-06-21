(ns ^{:doc "Utility macros for creating optimized operations for arrays."
      :author "EHF"}
  array-utils.core)

(set! *warn-on-reflection* true)

;; # Core utils

;; A note on bindings: All these macros use binding forms that look like:
;;   [[i x] xs
;;    y ys ...]
;; This binds x and y to the ith element of xs and ys, respectively. You can
;; include i-variables wherever and whenever you want, so you can do:
;;   [x xs
;;    y ys ...]
;; or:
;;   [[i1 x] xs
;;    [i2 y] ys
;;    [i3 z] zs ...]
;; but i1, i2, and i3 will have the same value.

;; All of these are internal tools that require type information as their first argument.
;; Refer to type_impl.clj, double.clj, long.clj et cetera for proper implementations.

(defn- intcast
  "Generate code to cast a symbol to an integer."
  [sym]
  `(clojure.lang.RT/uncheckedIntCast ~sym))

(defn typed-gensym
  "Generate a type-hinted symbol."
  [basis {:keys [atype] :as type-info}]
  (with-meta (gensym basis) {:tag atype}))

(defn- rebind-arrays
  "Given a type and a vector of array bindings, generate symbols for the
  arrays and array bindings to the new symbols.

  This is needed so that we never duplicate the instantiation of some array
  when someone types:
    (afill! [a (make-array)] a)
  If we didn't rebind, we might generate code like:
    (dotimes [i (alength (make-array))]
      (aset (make-array) i (aget (make-array) i)))"
  [type-info bindings]
  (assert (even? (count bindings))
          "Array binding requires an even number of forms")
  (let [full-info (for [[i-and-name arr] (partition 2 bindings)]
                    [i-and-name
                     ;; Add type-hint metadata.
                     (typed-gensym 'arr type-info)
                     arr])]
    [(vec (mapcat (fn [[_ arrsym arr]] [arrsym arr]) full-info))
     (vec (mapcat (fn [[i-and-name arrsym _]] [i-and-name arrsym]) full-info))]))

(defmacro abind
  "Given bindings of the form [[idx var] array ...], binds `idx` to the current
  index and `var` to the value at that index. Given only `[var array]`, binds
  `var` to the value at that index."
  [bindings i & body]
  (assert (even? (count bindings))
          "Array binding requires an even number of forms")
  (let [array-binding (fn [[i-and-name arr]]
                        (if (vector? i-and-name)
                          [(first i-and-name) i
                           (second i-and-name) `(aget ~arr ~(intcast i))]
                          [i-and-name `(aget ~arr ~(intcast i))]))]
    `(let ~(vec (mapcat array-binding (partition 2 bindings)))
       ~@body)))

(defmacro areduce-hint
  "Given bindings of the form [[idx var] array ...], reduces body over variable
  ret initialized to init."
  [type-info bindings ret init body]
  (let [[arr-rebindings bindings] (rebind-arrays type-info bindings)
        arr (second bindings)]
    `(let ~arr-rebindings
       (areduce ~arr i# ~ret ~init
                (abind ~bindings i# ~body)))))

;; for doing ops on a subset of an array
(defmacro doarr-bound-hint
  "Given bindings of the form [[idx var] array ...], performs body statements
  with bindings for each element of the array in the given bounds."
  [type-info [start stop] bindings & body]
  (let [[arr-rebindings bindings] (rebind-arrays type-info bindings)]
    `(let ~arr-rebindings
       (loop [i# (long ~start)]
         (when-not (== ~stop i#)
           (abind ~bindings i# ~@body)
           (recur (unchecked-inc-int i#)))))))

(defmacro doarr-hint
  "Given bindings of the form [[idx var] array ...], performs body statements
  with bindings for each element of the array."
  [type-info bindings & body]
  (let [arr (typed-gensym 'arr type-info)]
    `(let [~arr ~(second bindings)]
       (doarr-bound-hint ~type-info
                         [0 (alength ~arr)]
                         ~(assoc bindings 1 arr) ~@body))))

(defmacro afill-bound-into-hint!
  "Helper: Given bindings of the form [[idx var] array ...], maps body into the
  given array for each element of the input arrays in the given bounds."
  [{:keys [sg] :as type-info} dest [start stop] bindings body]
  (if (symbol? (first bindings))
    `(afill-bound-into-hint!
       ~type-info ~dest ~[start stop]
       ~(assoc bindings 0 [(gensym 'i) (first bindings)])
       ~body)
    (let [[arr-rebindings bindings] (rebind-arrays type-info bindings)
          i (ffirst bindings)]
      `(let ~arr-rebindings
         (doarr-bound-hint ~type-info
                           [~start ~stop]
                           ~bindings
                           (aset ~dest ~(intcast i) (~sg ~body)))))))

;; NOTE: clojure.core/amap is slow.
(defmacro amap-hint
  "Given bindings of the form [[idx var] array ...], maps body into a
  new array for each element of the input arrays."
  [{:keys [constructor] :as type-info} bindings body]
  (let [arr (typed-gensym 'arr type-info)
        anew (typed-gensym 'anew type-info)]
    `(let [~arr ~(second bindings)
           ~anew (~constructor (alength ~arr))]
       (afill-bound-into-hint! ~type-info ~anew
                               [0 (alength ~arr)]
                               ~(assoc bindings 1 arr) ~body))))

(defmacro afill-hint!
  "Given bindings of the form [[idx var] array ...], maps body intothe first
  array (destructively!) for each element of the input arrays."
  [type-info bindings body]
  (let [arr (typed-gensym 'a type-info)]
    `(let [~arr ~(second bindings)]
       (afill-bound-into-hint! ~type-info ~arr
                               [0 (alength ~arr)]
                               ~(assoc bindings 1 arr) ~body))))

(defmacro afill-bound-hint!
  "Given bindings of the form [[idx var] array ...], maps body intothe first
  array (destructively!) for each element of the input arrays in the bounds."
  [type-info [start stop] bindings body]
  (let [arr (typed-gensym 'a type-info)]
    `(let [~arr ~(second bindings)]
       (afill-bound-into-hint! ~type-info ~arr
                               ~[start stop]
                               ~(assoc bindings 1 arr) ~body))))

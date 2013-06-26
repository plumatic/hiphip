(ns array-utils.core
  "A note on bindings: All these macros use binding forms that look like:

  [[i x] xs
   y ys
   ...]

  This binds i to the index and x and y to the ith element of xs and ys,
  respectively. You can include index variables wherever and whenever you
  want, so you can do:

  [x xs
   y ys
   ...]

  or:

  [[i1 x] xs
   [i2 y] ys
   [i3 z] zs
   ...]

  but i1, i2, and i3 will have the same value.

  You can also include a range:

  [[i x] xs
   :range 0 10]

  and the operation will only be applied over that range.

  All of these are internal tools that require type information as
  their first argument. Refer to type_impl.clj, double.clj, long.clj
  et cetera for proper implementations.")

(set! *warn-on-reflection* true)

(defn- intcast
  "Generate code to cast a symbol to an integer."
  [sym]
  `(clojure.lang.RT/uncheckedIntCast ~sym))

(defn typed-gensym
  "Generate a type-hinted symbol."
  [basis tag]
  (with-meta (gensym basis) {:tag tag}))

(defn- get-range
  "Get the :range option out of the bindings."
  [bindings]
  (:range (apply hash-map bindings)))

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
  (->>
   ;; Bindings for each array
   (for [[i-and-name arr] (partition 2 bindings)]
     (if (= :range i-and-name)
       (let [start-sym (gensym 'start)
             stop-sym (gensym 'stop)]
         [[[start-sym stop-sym] arr]
          [:range [start-sym stop-sym]]])
       (let [sym (typed-gensym 'arr (:atype type-info))]
         [[sym arr] [i-and-name sym]])))
   ;; Transpose
   (apply map vector)
   ;; Concatenate
   (map #(vec (apply concat %)))))

(defmacro abind
  "Given bindings of the form [[idx var] array ...], binds `idx` to the current
  index and `var` to the value at that index. Given only `[var array]`, binds
  `var` to the value at that index."
  [bindings i & body]
  (assert (even? (count bindings))
          "Array binding requires an even number of forms")
  (let [array-binding
        (fn [[i-and-name arr]]
          (cond
           (vector? i-and-name) [(first i-and-name) i
                                 (second i-and-name) `(aget ~arr ~(intcast i))]
           (symbol? i-and-name) [i-and-name `(aget ~arr ~(intcast i))]
           ;; Ignore keywords like :range
           (keyword? i-and-name) nil
           :else (assert false
                         (format "Bad array binding form: %s" i-and-name))))]
    `(let ~(vec (mapcat array-binding (partition 2 bindings)))
       ~@body)))

(defmacro areduce-hint
  "Given bindings of the form [[idx var] array ...], reduces body over variable
  ret initialized to init."
  [type-info bindings ret init body]
  (let [[arr-rebindings bindings] (rebind-arrays type-info bindings)
        [start stop] (for [s (or (get-range bindings)
                                 [0 `(alength ~(second bindings))])]
                       `(long ~s))]
    `(let ~arr-rebindings
       (loop [i# ~start ~ret ~init]
         (if (< i# ~stop)
           (recur (unchecked-inc-int i#) (abind ~bindings i# ~body))
           ~ret)))))

(defmacro doarr-hint
  "Given bindings of the form [[idx var] array ...], performs body statements
  with bindings for each element of the array (in the given bounds)."
  [type-info bindings & body]
  (let [[arr-rebindings bindings] (rebind-arrays type-info bindings)
        [start stop] (for [s (or (get-range bindings)
                                 [0 `(alength ~(second bindings))])]
                       `(long ~s))]
    `(let ~arr-rebindings
       (loop [i# ~start]
         (when (< i# ~stop)
           (abind ~bindings i# ~@body)
           (recur (unchecked-inc-int i#)))))))

(defmacro afill-into-hint!
  "Helper: Given bindings of the form [[idx var] array ...], maps body into the
  given array for each element of the input arrays (in the given bounds)."
  [{:keys [sg] :as type-info} dest bindings body]
  (if (symbol? (first bindings))
    `(afill-into-hint!
      ~type-info ~dest
      ~(assoc bindings 0 [(gensym 'i) (first bindings)])
      ~body)
    (let [[arr-rebindings bindings] (rebind-arrays type-info bindings)
          i (ffirst bindings)]
      `(let ~arr-rebindings
         (doarr-hint ~type-info
                     ~bindings
                     (aset ~dest ~(intcast i) (~sg ~body)))))))

;; NOTE: clojure.core/amap is slow.
(defmacro amap-hint
  "Given bindings of the form [[idx var] array ...], maps body into a new array
  for each element of the input arrays."
  [{:keys [constructor] :as type-info} bindings body]
  (let [arr (typed-gensym 'arr (:atype type-info))
        anew (typed-gensym 'anew (:atype type-info))]
    `(let [~arr ~(second bindings)
           ~anew (~constructor (alength ~arr))]
       (afill-into-hint! ~type-info ~anew
                         ~(assoc bindings 1 arr) ~body))))

(defmacro afill-hint!
  "Given bindings of the form [[idx var] array ...], maps body into the first
  array (destructively!) for each element of the input arrays."
  [type-info bindings body]
  (let [arr (typed-gensym 'a (:atype type-info))]
    `(let [~arr ~(second bindings)]
       (afill-into-hint! ~type-info ~arr
                         ~(assoc bindings 1 arr) ~body))))

(ns hiphip.impl.core
  "Internal helpers for hiphip, including generating primitive
   type-specific array code and parsing hiphip-style array bindings."
  (:import [clojure.lang RT]))

(set! *warn-on-reflection* true)

(defmacro assert-iae
  "Like assert, but throws an IllegalArgumentException not an Error (and also takes args to format)"
  [form & format-args]
  `(when-not ~form (throw (IllegalArgumentException. (format ~@format-args)))))

(defn- find-shadows
  "Finds common variables in two bindings"
  [a b]
  (let [[a-vars b-vars] (map (partial take-nth 2) [a b])]
    (some (set a-vars) b-vars)))

(defn intcast
  "Generate code to cast a symbol to an integer."
  [sym]
  `(clojure.lang.RT/uncheckedIntCast ~sym))

(defn typed-gensym
  "Generate a type-hinted symbol."
  [basis tag]
  (with-meta (gensym basis) {:tag tag}))

(defn primitive-type-info
  "Produce an map of helpers for an array type"
  [type]
  (case type
    (double Double/TYPE clojure.core/double) {:array-tag 'doubles
                                              :unchecked-cast `RT/uncheckedDoubleCast
                                              :constructor `double-array}
    (float Float/TYPE clojure.core/float) {:array-tag 'floats
                                           :unchecked-cast `RT/uncheckedFloatCast
                                           :constructor `float-array}
    (long Long/TYPE clojure.core/long) {:array-tag 'longs
                                        :unchecked-cast `RT/uncheckedLongCast
                                        :constructor `long-array}
    (int Integer/TYPE clojure.core/int) {:array-tag 'ints
                                         :unchecked-cast `RT/uncheckedIntCast
                                         :constructor `int-array}
    (short Short/TYPE clojure.core/short) {:array-tag 'shorts
                                           :unchecked-cast `RT/uncheckedShortCast
                                           :constructor `short-array}
    (byte Byte/TYPE clojure.core/byte) {:array-tag 'bytes
                                        :unchecked-cast `RT/uncheckedByteCast
                                        :constructor `byte-array}
    (char Character/TYPE clojure.core/char) {:array-tag 'chars
                                             :unchecked-cast `RT/uncheckedCharCast
                                             :constructor `char-array}
    (boolean Boolean/TYPE clojure.core/boolean) {:array-tag 'booleans
                                                 :unchecked-cast `RT/booleanCast
                                                 :constructor `boolean-array}
    nil))

(defn array-cast
  "Produce an array hint for a primitive array expr of a given type"
  [type expr]
  (let [type-info (primitive-type-info type)]
    (assert type-info)
    (with-meta expr {:tag (:array-tag type-info)})))

(defn value-cast
  "Produce an unchecked cast for the value of a given type"
  [type expr]
  (if-let [type-info (primitive-type-info type)]
    `(~(:unchecked-cast type-info) ~expr)
    expr))

(defn parse-binding [index-sym [left right]]
  (case left
    :let {:let-bindings right}
    :range (do (assert-iae (and (vector? right) (= (count right) 2))
                           "Invalid range binding %s; must look like :range [10 20]" right)
               {:range-exprs right})
    ;; else
    (let [[idx-sym val-sym] (if (symbol? left)
                              [nil left]
                              (do (assert-iae (and (vector? left) (every? symbol left))
                                              "Invalid array binding %s: must be either a %s"
                                              left
                                              "val sym or pair of index and value syms")
                                  [(first left) (second left)]))
          array-sym (gensym "arr")]
      {:array-bindings [(vary-meta array-sym merge (select-keys (meta right) [:tag]))
                        right]
       :value-bindings (into (if idx-sym [idx-sym index-sym] [])
                             [val-sym `(aget ~array-sym ~(intcast index-sym))])})))

(defn parse-bindings
  "Given a type, index symbol, and a vector of array bindings, generate a map with keys:
   :start-sym - a symbol bound to the iteration start point
   :stop-sym - a symbol bound to the iteration stop point
   :initial-bindings - bindings [array-sym array-expr ...
                                 start-sym ...
                                 stop-sym ...]
    -- with array-sysm in the order provided in the input.
   :value-bindings - bindings [array-val array-sym ...
                               extra-index-sym index-sym]"
  [bindings]
  (assert-iae (even? (count bindings))
              "Array binding %s requires an even number of forms" bindings)
  (let [index-sym (gensym "i")
        start-sym (typed-gensym "start-sym" long)
        stop-sym (typed-gensym "stop-sym" long)
        {:keys [range-exprs
                array-bindings
                value-bindings
                let-bindings]} (->> bindings
                                    (partition 2)
                                    (map #(parse-binding index-sym %))
                                    (apply merge-with (comp vec concat)))
        [start-expr stop-expr] (cond (empty? range-exprs)
                                     [0 `(alength ~(first array-bindings))]

                                     (= 2 (count range-exprs))
                                     range-exprs

                                     :else
                                     (assert-iae false "Binding has multiple range exprs: %s"
                                                 bindings))]
    (assert-iae (seq array-bindings) "Bindings must include at least one array")
    ;; Do some analysis in case there are conflicting variables
    (let [shadows (find-shadows value-bindings let-bindings)]
      (assert-iae (not shadows)
                  "Variable `%s` shadowed by the let-binding in %s"
                  shadows
                  bindings))
    (assert-iae (or (nil? let-bindings) (and (vector? let-bindings) (even? (count let-bindings))))
                "Invalid let bindings %s; must look like :let [a 1 b 2]" let-bindings)
    {:index-sym index-sym
     :start-sym start-sym
     :stop-sym stop-sym
     :initial-bindings (into array-bindings
                             [start-sym start-expr stop-sym stop-expr])
     :value-bindings (into value-bindings let-bindings)}))

(defn hint-binding [type [left right]]
  (case left
    :range [:range right]
    :let [:let (->> (partition 2 right)
                    (mapcat (fn [[sym val]] `[~sym ~(value-cast type val)]))
                    vec)]
    [left (array-cast type right)]))

(defn hint-bindings [type bindings]
  (assert-iae (even? (count bindings))
              "Array binding %s requires an even number of forms" bindings)
  (->> (partition 2 bindings)
       (mapcat (partial hint-binding type))
       vec))

(defmacro dotimes-int
  "Like dotimes, but faster and only works on int ranges.  Also takes an optional
   start for the iteration."
  [[sym & start-stop] & body]
  (let [[start stop] (case (count start-stop)
                       1 (cons 0 start-stop)
                       2 start-stop)]
    `(let [stop# (long ~stop)]
       (loop [~sym (long ~start)]
         (when (< ~sym stop#)
           ~@body
           (recur (unchecked-inc-int ~sym)))))))

(defn ^String slurp-from-classpath [^String file]
  (slurp (.getResourceAsStream (clojure.lang.RT/baseLoader) file)))

(set! *warn-on-reflection* false)

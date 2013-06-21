(ns ^{:doc "Utility macros for creating HOFs for arrays."
      :author "EHF"}
  array-utils.core) 

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)
;; enable on pain of (REPL) death
(set! *print-length* 15) 

;; # Core utils

;; All of these are constructors. Refer to double.clj, long.clj et
;; cetera for proper implementations.

;; Accepts singular and plural forms of the casting function, for
;; example [long longs]. This would have been much nicer using a map,
;; but map destructuring doesn't seem to work in macros. When a set-fn
;; is required, it's aset-double, aset-long etc.

(defmacro abind-hint
  "Given bindings of the form [[idx var] array], binds `idx` to the
  current index and `var` to the value at that index. Given only
  `[var array]`, binds `var` to the value at that index."
  [[sg pl] bindings i & body]
  `(let ~(reduce
          (fn [init [name arr]]
            (if-not (symbol? name)
              (conj init name (vector `(~sg ~i)
                                      `(aget (~pl ~arr) ~i)))
              (conj init name `(aget (~pl ~arr) ~i))))
          [] (partition 2 bindings))
     ~@body))

(defmacro reduce-with-hint
  [[sg pl] [f unit] bindings & body]
  (let [arr `(~pl ~(second bindings))] ;; => 1000x perf
    `(areduce ~arr i# ret# ~unit
              (~f ret#
                  (abind-hint [~sg ~pl] ~bindings i# ~@body)))))

(defmacro doarr-hint
  [[sg pl] bindings & body]
  (let [arr `(~pl ~(second bindings))]
    `(dotimes [i# (alength ~arr)]
       (abind-hint [~sg ~pl] ~bindings i# ~@body))))

;; for doing ops on a subset of an array
(defmacro doarr-bound-hint
  [[sg pl] [start stop] bindings & body]
  (let [arr `(~pl ~(second bindings))]
    `(loop [i# (int ~start)]
       (when-not (== ~stop i#)
         (abind-hint [~sg ~pl] ~bindings i# ~@body)
         (recur (unchecked-inc-int i#))))))

(defmacro amap-hint
  [[sg pl] binding & body]
  (let [arr `(~pl ~(second binding))] 
    `(clojure.core/amap ~arr i# ret#
                        (~sg (abind-hint [~sg ~pl] ~binding i# ~@body)))))

;; Don't ask.

(defmacro afill-hint!
  [[sg pl set-fn] bindings & body]
  (if (symbol? (first bindings))
    `(afill-hint! [~sg ~pl ~set-fn]
                  [[i# ~(first bindings)] ~(second bindings)
                   ~@(drop 2 bindings)] ~@body)
    `(doarr-hint [~sg ~pl]
                 ~bindings
                 (~set-fn ~@((juxt second ffirst) bindings) ~@body))))

(defmacro afill-bound-hint!
  [[sg pl set-fn] [start stop] bindings & body]
  (if (symbol? (first bindings))
    `(afill-bound-hint! [~sg ~pl ~set-fn]
                        [~start ~stop]
                        [[i# ~(first bindings)] ~(second bindings)
                         ~@(drop 2 bindings)] ~@body)
    `(doarr-bound-hint [~sg ~pl]
                       [~start ~stop]
                       ~bindings
                       (~set-fn ~@((juxt second ffirst) bindings) ~@body))))
(ns ^{:doc
      "Example usage of array-utils.core."
      :author "EHF"}
  array-utils.examples
  (:refer-clojure :exclude [amap])
  (:use array-utils.double
        plumbing.core)
  (:require [array-utils.long :as l]
            [plumbing.graph :as graph]
            [criterium.core :as bench])
  (import [org.apache.commons.math3.special Gamma]))

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)
;; enable on pain of (REPL) death
(set! *print-length* 15)

;; # Examples

;; ~18 us (quick), 18 us (long)
(defn dot-product [ws xs]
  (asum [w ws x xs] (* w x)))

;; ~2.1 ms (quick), 2.2 ms (long)
(defn dot-product-boxed [ws xs]
  (apply + (map * xs ws)))

;; ------------------------

;; ## Statistics = fun!

(defn mean [xs]
  (/ (asum xs) (alength ^doubles xs)))

;; Consider writing a one-pass version. ~29 us (quick), 23 us (long)
(defn stddev
  "Calculates the standard deviation of xs."
  [xs]
  (let [s0 (alength ^doubles xs)
        s1 (asum xs)
        s2 (asum [x xs] (* x x))] ;; Math/pow has HUGE overhead ~ 200 us
    (/ (Math/sqrt (- (* s0 s2) (* s1 s1))) s0)))

;; ~900 ms (long)
(defn stddev-boxed [xs]
  (let [s0 (count xs)
        s1 (apply + xs)
        s2 (apply + (map #(* % %) xs))]
    (/ (Math/sqrt (- (* s0 s2) (* s1 s1))) s0)))

;; Hard to beat apache.commons, even with series expansion.
(defn digamma [x] (Gamma/digamma x))

;; usually takes ~1.0-5.5 ms.
(defn exp-log-probs [alphas]
  (let [log-z (digamma (asum alphas))]
    (amap [a alphas]
          (- (digamma a) log-z))))

;; Look ma, in-place!
(defn exp-log-probs! [alphas]
  (let [log-z (digamma (asum alphas))]
    (afill! [a alphas]
            (- (digamma a) log-z))))

(defn var-probs [alphas]
  (let [a0 (asum alphas)
        m (* (* a0 a0) (inc a0))]
    (amap [a alphas]
          (/ (* a (- a0 a)) m))))

;; ------------------------

;; ## Adding up time

;; Do you hate adding up hours? Not anymore! ~16 us.
(defn sum-hours
  "Sums up 'metric' hours of time and returns the time."
  [hours]
  (rem (asum hours) (double 24)))

(comment
  (def hours (double-array (repeatedly 10e3 #(rand-int 24))))

  (def hours (long-array (repeatedly 10e3 #(rand-int 24))))

  (defn sum-hours-long
    "Sums up 'metric' hours of time and returns the time."
    [hours]
    (rem (l/asum hours) (long 24)))

  (sum-hours-long hours)

  )

;; ------------------------

;; ## Norwegian PINs

;; ~1.6 us (quick, long)
(defn valid-personal-number?
  "Verifies a Norwegian personal number."
  [xs]
  (let [m1 (long-array [3 7 6 1 8 9 4 5 2 0 0]) ;; last two for identity
        m2 (long-array [5 4 3 2 7 6 5 4 3 2 0])
        k1 (- 11 (mod (l/asum [x xs const m1] (* x const)) 11))
        k2 (- 11 (mod (l/asum [x xs const m2] (* x const)) 11))
        [n1 n2] (take-last 2 xs)]
    (and (== n1 k1) (== n2 k2))))

;; ~7.9 us (quick), ~7.5 us (long)
(defn valid-pn-naive? [xs]
  (let [m1 [3 7 6 1 8 9 4 5 2 0 0] ;; last two for identity
        m2 [5 4 3 2 7 6 5 4 3 2 0]
        check-fn (fn [ms]
                   (- 11.0 (mod (apply + (map * ms xs)) 11)))
        [k1 k2] (map check-fn [m1 m2])
        [n1 n2] (take-last 2 xs)]
    (and (== n1 k1) (== n2 k2))))

;; ------------------------

;; ## Applied geology

;; Rock quality designation (RQD) denotes the degree of jointing or
;; fracture in a rock mass. It is used in the rock mass rating (RMR)
;; to give a rough measure of the characteristics of the rock.

;; 2.5 ms. Slow, but probably fast enough. Can we do better?
(defn RQD-boxed [xs core-diameter]
  (let [ys (filter (partial < (* 2 core-diameter)) xs)]
    (* 100 (/ (apply + ys) (apply + xs)))))

;;  1.9 ms
#_(defn RQD-array
  [xs core-diameter]
  (let [ys (afilter (partial < (* 2 core-diameter)) xs)]
    (* 100.0 (/ (asum ys) (asum xs)))))

;; ~220 us. Getting better. The major bottleneck turns out to be
;; `partial`, which is really slow for some reason.
#_(defn RQD-afilter
  [xs core-diameter]
  (let [^double ys (afilter #(< (* 2.0 core-diameter) %) xs 0.0)]
    (* 100.0 (/ (asum ys) (asum xs)))))

;; Can we go faster? Sure. 76 us.
(defn RQD-fast
  [xs core-diameter]
  (let [ys (amap [x xs] (if (< (* 2.0 core-diameter) x) x 0.0))]
    (* 100.0 (/ (asum ys) (asum xs)))))

;; Even faster. 38 us.
(defn RQD
  [xs core-diameter]
  (let [ys-sum (asum [x xs] (if (< (* 2.0 core-diameter) x) x 0.0))]
    (* 100.0 (/ ys-sum (asum xs)))))

;; In review: For very tight loops, be aware of how inlining
;; operations can allow the JVM to do really smart stuff. There is no
;; silver bullet, of course, but it somewhat easy to do with such nice
;; abstractions over primitive array operations.

;; ## Time for Praxis

;; Now, let's put everything together and do something mildy
;; interesting. Or at least wacky and somewhat complex.

(comment

  (def my-array (double-array (repeatedly 10e3 rand)))

  (def stats-graph
    {:rqd  (fnk [xs] (RQD xs 0.04))
     :probs  (fnk [xs] (exp-log-probs xs))
     :std (fnk [xs] (stddev xs))
     :rqd-digamma (fnk [rqd] (digamma rqd))
     :ass (fnk [xs] (asum [x xs] x))
     :dot-product  (fnk [xs probs] (dot-product xs probs))})

  (def stats-eager (graph/eager-compile stats-graph))

  (stats-eager {:xs my-array})

  ;; => {:rqd-digamma 4.593293092502177, :std 0.2894233837513104, :rqd
  ;;    99.31889445432603, :dot-product -51499.834485766805, :probs
  ;;    #<double[] [D@1cb8313>}

  (def profiled-stats (graph/eager-compile (graph/profiled ::profile-data stats-graph)))

  ;; Look ma, benchmarks!
  (def res @(::profile-data (profiled-stats {:xs my-array})))

  ;; =>
  {:rqd-digamma 0.003694, :std 0.383838, :rqd 0.17283,
   :dot-product 0.049673, :probs 4.732489}

  ;; and the total running time is . . .
  (apply + (vals res))

  ;; => ~5.3 ms.

  )

;; ## What does this tell us?
;;
;; The benchmarks above haven't been done against tons of varied data.
;; It is thus possible we have reached local maxima for some of them.
;; Still, note the following:
;;
;; * The dot product is about as fast as Apache Common's digamma,
;; which is awesome, to say the least.

;; * Total execution time is about 5 milliseconds.

;; * Corollary: Reflection is (probably) eliminated for most cases,
;; even though the functions make little or no use of type hints. This
;; allows for more declarative programming and less type-hint hacking.
;;
;; * 7 macros and 21 functions, showing that it is easy to build upon the
;; primitives offered by this session.
;;
;; * We are able to compose them using Graph, suggesting they are
;; indeed somewhat composable. (No guarantees, though!)

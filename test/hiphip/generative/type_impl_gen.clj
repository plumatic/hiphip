(defspec asum-sums
  (fn [xs]
    (asum [x xs] x))
  [^{:tag (`array-gen)} xs]
  (assert (= % (apply + xs))))

(defspec asum-sums-range
  (fn [xs]
    (asum [x xs :range [50 100]] x))
  [^{:tag (`array-gen)} xs]
  (assert (= % (apply + (take 50 (drop 50 xs))))))

(defspec dot-product-dots
  (fn [xs ys]
    (dot-product xs ys))
  [^{:tag (`array-gen)} a ^{:tag (`array-gen)} b]
  (do
    ;; This is commented out due to Clojure's math ops returning longs
    ;; or doubles, even when integers are involved. - emilhf.
    ;; (assert (is-type? %))
    (assert (= % (reduce + (map * a b))))))

(defspec amean-finds-the-mean
  (fn [xs]
    (amean xs))
  [^{:tag (`array-gen)} xs]
  (assert (= (/ (double (reduce + xs)) (count xs)) %)))

(defspec amax-returns-the-largest-number
  (fn [a]
    (amax a))
  [^{:tag (`array-gen)} a]
  (assert (= (reduce max a) %)))

(defspec amin-returns-the-smallest-number
  (fn [a]
    (amin a))
  [^{:tag (`array-gen)} a]
  (assert (= (reduce min a) %)))

(defspec amap-maps
  (fn [xs]
    (amap [[i x] xs] (+ i x)))
  [^{:tag (`range-gen)} xs]
  (do
    (assert (every? true? (map == xs (range 10e3))))
    (assert (every? true? (map == % (for [i (range 10e3)] (* 2 i)))))))

(defspec afill!-does-in-place-replacement
  (fn [xs]
    (let [ys (new-array (alength xs))]
      (afill! [[i y] ys]
              (aget xs i))
      ys))
  [^{:tag (`array-gen)} xs]
  (assert (every? true? (map = xs %))))

(defspec afill!-only-mutates-the-first-array
  (fn [xs]
    (let [old-xs (aclone xs)
          ys (new-array (alength xs))]
      (afill! [[i y] ys b xs] b)
      old-xs))
  [^{:tag (`array-gen)} xs]
  (assert (every? true? (map = xs %))))

(defspec afill!-replaces-interval
  (fn [xs]
    (afill! [x xs :range [1 4]] 2)
    xs)
  [^{:tag (`range-gen)} xs]
  (assert (every? true? (map == (concat [0.0 2.0 2.0 2.0] (range 4 10e3)) xs))))

(defspec doarr-has-side-effects
  (fn [xs]
    (let [ys (new-array (alength xs))]
      (doarr [[i y] ys x xs]
             (aset ys i x))
      ys))
  [^{:tag (`array-gen)} xs]
  (assert (every? true? (map = xs %))))
array-utils
===========

`array-utils` is an array macro library for Clojure. It offers more
elegant ways to handle primitive arrays, including both functional forms
like `amap` and `areduce`, and in-place forms like `afill!`.

The functions and macros require little or no manual type hinting, and
they use a binding semantics similar to those of for (see below).

`array-utils` supports multiple array types, including floats, doubles,
ints, and longs. Feel free to make your own implementations using the
abstractions offered in `core.clj` and `type_impl.clj`.

Note: if you don't need the speed of primitive arrays, we encourage you
to keep using Clojure's 'map' and 'reduce'--they're nicer except for the
speed.

# Usage

In your `project.clj`, add a dependency on
`[array-utils "unreleased-version"]`. Then require a type namespace,
e.g.
```clojure
(require 'array-utils.double)
```

# Motivation

Instead of writing

```clojure
(defn dot-product [^doubles ws ^doubles xs]
  (areduce xs i ret 0.0
    (+ ret (* (aget xs i)
              (aget ws i))))
```

you can write

```clojure
(defn dot-product [ws xs] (au/asum [x xs w ws] (* x w)))
```

# Bindings

All these macros use binding forms that look like:
```clojure
(au/amap
  [[i x] xs
   y ys ...]
  <expression involving i, x, y>)
```

This binds x and y to the ith element of xs and ys, respectively. You
can include i-variables wherever and whenever you want, so you can do:
```clojure
(au/amap
  [x xs
   y ys ...]
  <expression involving x, y>)
```
or:
```clojure
(au/amap
  [[i1 x] xs
   [i2 y] ys
   [i3 z] zs ...]
  <expression involving i1, x, i2, y, i3, z>)
```
but i1, i2, and i3 will have the same value.

# Examples

## areduce

```clojure
(au/areduce [x xs y ys] result 1 (max result (/ x y)))
```

## asum

```clojure
;; dot product
(au/asum [x xs y ys] (* x y))
;; Compute a power series.
(let [x 2.0]
  (au/asum [[i a] as] (* a (Math/pow x i))))
```

## aproduct

```clojure
;; Compute a probability
(let [scale 3.0]
  (au/aproduct [x xs] (/ x scale)))
```

## amap

```clojure
;; Take the max of two arrays.
(au/amap [x xs y ys] (max x y))
```

## afill!

```clojure
;; Add a constant to an array.
(au/afill! [x xs] (+ x 1.0))
;; The += operation for arrays.
(au/afill! [x xs y ys] (+ x y))
;; Insert marker values for each negative x.
(au/afill! [x xs] (if (< 0 x) 999 x))
```

## doarr

```clojure
;; Apply some java object's function.
(let [java-thing (JavaThing.)]
  (au/doarr [[i x] xs y ys] (.process java-thing i x y))
  (.getResult java-thing))
```

## Functions

There are also a few functions available:

```clojure
;; Maximum over an array
(au/amax xs)
;; Minimum over an array
(au/amin xs)
;; Average of an array
(au/amean xs)
;; Dot product of two arrays
(au/dot-product xs ys)
```

# License

Copyright © 2013 Emil Flakk, Leon Barrett, and others.

Distributed under the Eclipse Public License, the same as Clojure.

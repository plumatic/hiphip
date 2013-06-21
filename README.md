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
`[array-utils "unreleased-version"]`.

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
(defn dot-product [ws xs] (asum [x xs w ws] (* x w)))
```

See `examples.clj` for more.

# Bindings

All these macros use binding forms that look like:
```clojure
(amap
  [[i x] xs
   y ys ...]
  <expression involving i, x, y>)
```
This binds x and y to the ith element of xs and ys, respectively. You
can include i-variables wherever and whenever you want, so you can do:
```clojure
(amap
  [x xs
   y ys ...]
  <expression involving x, y>)
```
or:
```clojure
(amap
  [[i1 x] xs
   [i2 y] ys
   [i3 z] zs ...]
  <expression involving i1, x, i2, y, i3, z>)
```
but i1, i2, and i3 will have the same value.

# Documentation

See the docstrings. Generate the HTML documentation by running
`lein marg`.

# License

Copyright © 2013 Emil Flakk, Leon Barrett, and others.

Distributed under the Eclipse Public License, the same as Clojure.

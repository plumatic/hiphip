array-utils_internal
====================

Internal version of array-utils (new name tbd?)

# array-utils

`array-utils` is a small lib inspired by Prismatic's Flop. It offers
more elegant ways to handle primitive arrays. Includes functional
programming staples like `afilter` and `amap`, as well as in-place
versions like `afill!` and `afilter!`.

Most of the functions require little or no manual type hinting, and
most of them support binding semantics similar to for-each in
imperative languages. (See `core.clj/abind-hint`).

`array-utils` now supports multiple array types, starting with doubles
and longs. Feel free to make your own implementations using the
abstractions offered in `core.clj`.

**NB.** Please consider using built-in Clojure functions and data
structures unless you need the raw speed of Java primitives. Please
think of the children.

## Usage

Pull the repo, run `lein deps`, and require `array-utils.double`.

## Motivation

Instead of 

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

## Documentation

See the docstrings. Generate the HTML documentation by running `lein marg`.

## License

Copyright © 2013 Emil Flakk

Distributed under the Eclipse Public License, the same as Clojure.


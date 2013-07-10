# Developer notes

This is a short introduction to the library's inner workings. The
primary audience is people wanting to extend the API for themselves or
provide patches.

## The short gist

`hiphip` provides primed functions and macros, that is, code with the
necessary type hints to avoid reflection when dealing with primitive
arrays.

The core macros are found in `hiphip.array`. They accept the arguments
of their "primed" counterparts (e.g. `amap`), in addition to type info
needed set up the code. All bindings are parsed using `parse-bindings`
in `hiphip.core`, which returns a map of the necessary value bindings
and vars.

Most of our implementation code is contained in `type_impl.clj`. The
type namespaces (e.g. `hiphip.double`) contain all of the necessary
type info, and load `type_impl.clj`, which then extracts the necessary
type info and "primes" the API provided by `hiphip.array`.

A benchmarking suite (see the README) is provided to make sure
performance demands are met. If not, the tests fail. This is
implemented using a technique similar to the one in the type
namespaces. The functions are tested against Java equivalents and
results are given in relative speeds. Ideally most of our code should
be within ~1.2 of Java. (This is unfortunately not always the case).

## Extending hiphip

You should start with the macros provided in `hiphip.array` and build
from there. Provide the necessary type information to set up your
functions and macros. If you need your code to be shared between
different implementations, feel free to copy our approach by loading a
`type_impl.clj` in a type namespace.

## Contributing to hiphip

All contributors should run `lein test :bench` and `lein test :fast`
before submitting their pull requests. Performance regressions are not
acceptable at this point in time.

Implementations for new arrays should use the API provided by
`hiphip.array` unless unfeasible. New implementations should also be
added to the benchmark suit (with initial relative slowness) to track
their relative performance over time.

## Common caveats during development

* Setting `*unchecked-math*` to `true` might either improve and worsen
  performance, but never reliably so. This is normal, or rather, we
  don't really know what is going on.

* Beware of overflow when writing your tests. For large arrays, some
  types just can't handle the heat (or rather, large numbers).

* Make sure you have `:jvm-opts ^:replace []` in your `project.clj`.
  Otherwise benchmarks might seem to be regressing a lot.
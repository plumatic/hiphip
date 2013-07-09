# Developer notes

This is a short introduction to the library's inner workings. The
primary audience is people wanting to extend the API or provide
patches.

# The short gist

`hiphip` provides primed functions and macros, that is, code with the
necessary type hints to avoid reflection when dealing with primitive
arrays.

The main code is in `hiphip.array`. These implementations accept the
arguments of their "primed" counterparts (e.g. `amap`), in addition to
type info needed set up the code. All bindings are parsed using
`parse-bindings` in `hiphip.core`, which returns a map of the
necessary bindings and vars.

The type type namespaces (e.g. `hiphip.double`) contain `type-info`, a
hashmap with the necessary tags and functions to set up
`hiphip.array`. This is done by loading `type_impl.clj`, which
extracts the necessary type and "primes" the functions contained in
`hiphip.array`.

A benchmarking suite (runnable by calling `lein test`) is provided to
make sure performance demands are met. If not, the tests fail. This is
implemented using a technique similar to the one in the type
namespaces. The functions are tested against Java equivalents and
results are given in relative speeds. Ideally most of our code should
be within ~1.2 of Java. (This is unfortunately not always the case).

## Extending hiphip

If you're not looking to contribute your implementation upstream, the
easiest solution is to download `type_impl.clj` and adding your type
information on the top. These type information needs to contain th

## Contributing to hiphip

All contributors should run `lein test` before submitting their pull
requests. Regressions are not acceptable at this point in time.

Implementations for new arrays should use the functions in
`hiphip.array` unless unfeasible. New implementations should also be
added to the benchmark suit (with initial relative slowness) to track
their relative performance.
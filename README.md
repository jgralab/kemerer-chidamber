# kemerer-chidamber

This project implements the Kemerer & Chidamber OO-metrics for GraBaJa and
JaMoPP syntax graphs.

## Getting started

This project uses [Leiningen](https://github.com/technomancy/leiningen) for
retrieving all its dependencies from various Maven repositories, building, and
test automation.

Getting started is really simple:

- Install the `lein` shell (or bat) script as explained in the **Installation**
section of the [Leiningen](https://github.com/technomancy/leiningen) page.

- Fetch the project's dependencies:

```
$ cd kemerer-chidamber
$ lein deps
```

- Run the tests/benchmarks with:

```
$ lein test                                     # All benchmarks: GraBaJa & JaMoPP
$ lein test kemerer-chidamber.grabaja.test.core # Only GraBaJa
$ lein test kemerer-chidamber.jamopp.test.core  # Only JaMoPP
```

## License

Copyright (C) 2012 Tassilo Horn <horn@uni-koblenz.de>

Distributed under the General Public License (Version 3), with the following
additional grant:

```
Additional permission under GNU GPL version 3 section 7

If you modify this Program, or any covered work, by linking or combining it
with Eclipse (or a modified version of that program or an Eclipse plugin),
containing parts covered by the terms of the Eclipse Public License (EPL), the
licensors of this Program grant you additional permission to convey the
resulting work.  Corresponding Source for a non-source form of such a
combination shall include the source code for the parts of FunnyQT and JGraLab
used as well as that of the covered work.
```


<!-- Local Variables:        -->
<!-- mode: markdown          -->
<!-- indent-tabs-mode: nil   -->
<!-- End:                    -->

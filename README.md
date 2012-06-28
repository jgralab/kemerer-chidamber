# kemerer-chidamber

This project implements the Kemerer & Chidamber OO-metrics for GraBaJa and
JaMoPP syntax graphs.

## Getting started

This project uses [Leiningen](https://github.com/technomancy/leiningen) for
retrieving all its dependencies from various Maven repositories, building, and
test automation.

Getting started is really simple:

- Install the `lein` shell (or bat) script as explained at the
  [Leiningen homepage](http://leiningen.org) page.

- Fetch the project's dependencies:

```
$ cd kemerer-chidamber
$ lein deps
```

- Run the metrics suite on some GraBaJa or JaMoPP model.  For example, the
  following command would load the `my-model.xmi` JaMoPP model, calculate the
  metrics on every class in the `foo.bar` package (specified as a regex), and
  display the metric values for the 20 most complex classes per metric.

```
$ lein run my-model.xmi 'foo\.bar\..*' 20
```

### Generating a JaMoPP model

To generate a JaMoPP model of your own Java code so that you can calculate the
metrics for your own classes, too, do the following.

- Checkout the JaMoPPC command line tool from the EMFText Subversion
  repository.

```
$ svn co http://svn-st.inf.tu-dresden.de/svn/reuseware/trunk/EMFText%20Languages/org.emftext.language.java.jamoppc
```

- That checkout contains a runnable `jamoppc.jar` which you can use to generate
  a JaMoPP model from your Java source code.  The following command parses the
  code in the `src/` folder, generates a JaMoPP model of the complete project,
  and saves it as `my-project.xmi`.  The other arguments in the call refer
  external JAR libraries used by your project which are needed for resolution
  of classes, methods, and fields.

```
$ cd path/to/my-project
$ java -Xmx2G -jar path/to/jamoppc.jar src/ my-project.xmi \
    lib/foo.jar lib/bar.jar
```

- Now you can calculate the metrics as shown above.

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

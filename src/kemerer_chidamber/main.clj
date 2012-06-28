(ns kemerer-chidamber.main
  (:require [clojure.pprint :as pp])
  (:require [funnyqt.emf :as emf])
  (:require [funnyqt.tg :as tg])
  (:require [funnyqt.query.tg :as tgq])
  (:require [kemerer-chidamber.grabaja.core :as grabaja])
  (:require [kemerer-chidamber.jamopp.core :as jamopp]))

(defn grabaja-do [graph n rx]
  (binding [grabaja/*get-classes-fn* (fn [graph]
                                       (filter
                                        (fn [c] (re-matches rx (tg/value c :fullyQualifiedName)))
                                        (tgq/vseq graph 'ClassDefinition)))]
    (println "Depth of Inheritance Tree:")
    (println "==========================")
    (pp/pprint (take n (grabaja/classes-by-depth-of-inheritance-tree-forkjoin graph)))

    (println)
    (println "Coupling between Objects:")
    (println "=========================")
    (pp/pprint (take n (grabaja/classes-by-coupling-between-objects-forkjoin graph)))

    (println)
    (println "Weighted Methods per Class:")
    (println "===========================")
    (pp/pprint (take n (grabaja/classes-by-weighted-methods-per-class-forkjoin graph)))

    (println)
    (println "Number of Children:")
    (println "===================")
    (pp/pprint (take n (grabaja/classes-by-number-of-children-forkjoin graph)))

    (println)
    (println "Response for a Class:")
    (println "=====================")
    (pp/pprint (take n (grabaja/classes-by-response-for-a-class-forkjoin graph)))

    (println)
    (println "Lack of Cohesion in Methods:")
    (println "============================")
    (pp/pprint (take n (grabaja/classes-by-lack-of-cohesion-in-methods-forkjoin graph)))))

(defn jamopp-do [model n rx]
  (binding [jamopp/*get-classes-fn* (fn [m]
                                      (filter
                                       (fn [c]
                                         (re-matches (re-pattern rx)
                                                     (jamopp/class-qname c)))
                                       (emf/eallcontents m 'Class)))]
    (println "Depth of Inheritance Tree:")
    (println "==========================")
    (pp/pprint (take n (jamopp/classes-by-depth-of-inheritance-tree-forkjoin model)))

    (println)
    (println "Coupling between Objects:")
    (println "=========================")
    (pp/pprint (take n (jamopp/classes-by-coupling-between-objects-forkjoin model)))

    (println)
    (println "Weighted Methods per Class:")
    (println "===========================")
    (pp/pprint (take n (jamopp/classes-by-weighted-methods-per-class-forkjoin model)))

    (println)
    (println "Number of Children:")
    (println "===================")
    (pp/pprint (take n (jamopp/classes-by-number-of-children-forkjoin model)))

    (println)
    (println "Response for a Class:")
    (println "=====================")
    (pp/pprint (take n (jamopp/classes-by-response-for-a-class-forkjoin model)))

    (println)
    (println "Lack of Cohesion in Methods:")
    (println "============================")
    (pp/pprint (take n (jamopp/classes-by-lack-of-cohesion-in-methods-forkjoin model)))))

(def load-jamopp-mm (memoize (fn [] (emf/load-metamodel "java.ecore"))))

(defn -main [& args]
  (if-not (#{1 2 3} (count args))
    (println "Usage: lein run <syntax-graph-file> [<regex> [<no>]]\n
  syntax-graph-file: Either a GraBaJa TGraph file or a JaMoPP model.
              regex: A regular expression for filtering the relevant classes.
                     Defaults to '.*', i.e., all classes.
                 no: The number of classes to display with the highest complexity.
                     Defaults to 10.

  An example call might look like this:

    $ lein run my-project.xmi 'tld\\.domain\\.pkg\\..*' 50

  That would load the JaMoPP model my-project.xmi, calculate all metrics for
  every class in the tld.domain.pkg Java package, and display the results of
  the 50 most complex classes.")
    (let [[f rx n] args]
      (if (re-matches #".*\.tg(\.gz)?" f)
        (grabaja-do (tg/load-graph f)
                    (if n (Integer/parseInt n) 10)
                    (if rx (re-pattern rx) #".*"))
        (do
          (load-jamopp-mm)
          (jamopp-do (emf/load-model f)
                     (if n (Integer/parseInt n) 10)
                     (if rx (re-pattern rx) #".*")))))))

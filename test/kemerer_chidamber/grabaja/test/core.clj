(ns kemerer-chidamber.grabaja.test.core
  (:use [kemerer-chidamber.grabaja.core])
  (:use kemerer-chidamber.test.generic)
  (:use [funnyqt.tg.core])
  (:use [funnyqt.tg.query])
  (:use [clojure.test]))

(defn load-generic []
  (load-graph "jgralab.tg.gz"))

(defn load-standard []
  (load-graph "jgralab.tg.gz" :standard))

(deftest benchmark-generic-vs-standard
  (doseq [f [load-standard load-generic]]
    (let [g (f)
          iterations 2]
      (if (instance? de.uni_koblenz.jgralab.impl.generic.GenericGraphImpl g)
        (do
          (println "Running with a GENERIC graph")
          (println "============================"))
        (do
          (println "Running with a STANDARD graph")
          (println "=============================")))

      (binding [*get-classes-fn* (fn [graph]
                                   (filter
                                    (fn [c] (re-matches #"de\.uni_koblenz\.jgralab\..*"
                                                       (value c :fullyQualifiedName)))
                                    (vseq graph 'ClassDefinition)))]
        (dotimes [i iterations]
          (println)
          (println "Run" (inc i) "/" iterations)
          (println)
          (System/gc)

          (do-timing g "Depth of Inheritance Tree:"
                     classes-by-depth-of-inheritance-tree
                     classes-by-depth-of-inheritance-tree-forkjoin)

          (do-timing g "Coupling between Objects:"
                     classes-by-coupling-between-objects
                     classes-by-coupling-between-objects-forkjoin)

          (do-timing g "Weighted Methods per Class:"
                     classes-by-weighted-methods-per-class
                     classes-by-weighted-methods-per-class-forkjoin)

          (do-timing g "Number of Children:"
                     classes-by-number-of-children
                     classes-by-number-of-children-forkjoin)

          (do-timing g "Response for a Class:"
                     classes-by-response-for-a-class
                     classes-by-response-for-a-class-forkjoin)

          (do-timing g "Lack of Cohesion in Methods:"
                     classes-by-lack-of-cohesion-in-methods
                     classes-by-lack-of-cohesion-in-methods-forkjoin))))))

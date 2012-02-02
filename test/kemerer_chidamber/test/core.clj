(ns kemerer-chidamber.test.core
  (:use [kemerer-chidamber.core])
  (:use [funnyqt.tg.core])
  (:use [clojure.test]))

(defn load-generic []
  (load-graph "jgralab.tg.gz"))

(defn load-standard []
  (load-graph "jgralab.tg.gz" :standard))

(deftest benchmark-generic-vs-standard
  (doseq [f [load-standard load-generic]]
    (let [g (f)]
      (if (instance? de.uni_koblenz.jgralab.impl.generic.GenericGraphImpl g)
          (do
            (println "Running with a GENERIC graph")
            (println "============================"))
          (do
            (println "Running with a STANDARD graph")
            (println "=============================")))

      (dotimes [i 3]
        (println)
        (println "Run" (inc i) "/" 3)
        (println)
        (System/gc)

        (print "  Depth of Inheritance Tree:\t")
        (time (dorun (classes-by-depth-of-inheritance-tree g)))

        (print "  Coupling between Objects:\t")
        (time (dorun (classes-by-coupling-between-objects g)))

        (print "  Weighted Methods per Class:\t")
        (time (dorun (classes-by-weighted-methods-per-class g)))

        (print "  Number of Children:\t\t")
        (time (dorun (classes-by-number-of-children g)))

        (print "  Response for a Class:\t\t")
        (time (dorun (classes-by-response-for-a-class g)))

        (print "  Lack of Cohesion in Methods:\t")
        (time (dorun (classes-by-lack-of-cohesion-in-methods g)))))))

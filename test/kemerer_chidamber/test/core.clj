(ns kemerer-chidamber.test.core
  (:use [kemerer-chidamber.core])
  (:use [funnyqt.tg.core])
  (:use [clojure.test]))

(defn load-generic []
  (load-graph "jgralab.tg.gz"))

(defn load-standard []
  (load-graph "jgralab.tg.gz" :standard))

(defn do-timing [g title sv tpv]
  (println " " title)
  (print "    - Sequential: ")
  (let [r1 (time (doall (sv g)))]
    (print "    - ThreadPool: ")
    (let [r2 (time (doall (tpv g)))]
      (is (= r1 r2)))))

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

        (do-timing g "Depth of Inheritance Tree:"
                   classes-by-depth-of-inheritance-tree
                   classes-by-depth-of-inheritance-tree-parallel)

        (do-timing g "Coupling between Objects:"
                   classes-by-coupling-between-objects
                   classes-by-coupling-between-objects-parallel)

        (do-timing g "Weighted Methods per Class:"
                   classes-by-weighted-methods-per-class
                   classes-by-weighted-methods-per-class-parallel)

        (do-timing g "Number of Children:"
                   classes-by-number-of-children
                   classes-by-number-of-children-parallel)

        (do-timing g "Response for a Class:"
                   classes-by-response-for-a-class
                   classes-by-response-for-a-class-parallel)

        (do-timing g "Lack of Cohesion in Methods:"
                   classes-by-lack-of-cohesion-in-methods
                   classes-by-lack-of-cohesion-in-methods-parallel)))))

(ns kemerer-chidamber.tg.test.core
  (:use [kemerer-chidamber.tg.core])
  (:use [funnyqt.tg.core])
  (:use [clojure.test]))

(defn load-generic []
  (load-graph "jgralab.tg.gz"))

(defn load-standard []
  (load-graph "jgralab.tg.gz" :standard))

(defn do-timing [g title sv tpv fjv]
  (println " " title)
  (print "    - Sequential: ")
  (let [r1 (time (doall (sv g)))]
    (print "    - ThreadPool: ")
    (let [r2 (time (doall (tpv g)))]
      (print "    - ForkJoin:   ")
      (let [r3 (time (doall (fjv g)))]
        (is (= r1 r2 r3))))))

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
                   classes-by-depth-of-inheritance-tree-parallel
                   classes-by-depth-of-inheritance-tree-forkjoin)

        (do-timing g "Coupling between Objects:"
                   classes-by-coupling-between-objects
                   classes-by-coupling-between-objects-parallel
                   classes-by-coupling-between-objects-forkjoin)

        (do-timing g "Weighted Methods per Class:"
                   classes-by-weighted-methods-per-class
                   classes-by-weighted-methods-per-class-parallel
                   classes-by-weighted-methods-per-class-forkjoin)

        (do-timing g "Number of Children:"
                   classes-by-number-of-children
                   classes-by-number-of-children-parallel
                   classes-by-number-of-children-forkjoin)

        (do-timing g "Response for a Class:"
                   classes-by-response-for-a-class
                   classes-by-response-for-a-class-parallel
                   classes-by-response-for-a-class-forkjoin)

        (do-timing g "Lack of Cohesion in Methods:"
                   classes-by-lack-of-cohesion-in-methods
                   classes-by-lack-of-cohesion-in-methods-parallel
                   classes-by-lack-of-cohesion-in-methods-forkjoin)))))

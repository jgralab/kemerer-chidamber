(ns kemerer-chidamber.jamopp.test.core
  (:use kemerer-chidamber.jamopp.core)
  (:use kemerer-chidamber.test.generic)
  (:use funnyqt.emf)
  (:use funnyqt.query)
  (:use funnyqt.query.emf)
  (:use clojure.test)
  (:require [clojure.java.io :as io]))

(defn register-jamopp-metamodel
  [ecore-file]
  (load-metamodel ecore-file))

(register-jamopp-metamodel "layout.ecore")
(register-jamopp-metamodel "java.ecore")

(defn gunzip
  [fi fo]
  (with-open [i (io/reader
                 (java.util.zip.GZIPInputStream.
                  (io/input-stream fi)))
              o (java.io.PrintWriter. (io/writer fo))]
    (doseq [l (line-seq i)]
      (.println o l))))

(defn load-jgralab-model
  []
  (when-not (.exists (io/file "pcollections.xmi"))
    (println "Gunzipping pcollections.xmi.gz...")
    (gunzip "pcollections.xmi.gz" "pcollections.xmi"))
  (println "Loading JaMoPP model of pcollections...")
  (load-model "pcollections.xmi"))

(def jm (memoize load-jgralab-model))

(deftest benchmark
  (println "Running with a JaMoPP Model")
  (println "===========================")
  (let [m (jm)
        iterations 2]
    (binding [*get-classes-fn* (fn [m]
                                 (filter
                                  (fn [c]
                                    (re-matches #"org\.pcollections\..*"
                                                (classifier-qname c)))
                                  (eallcontents m 'Class)))]
      (dotimes [i iterations]
        (println)
        (println "Run" (inc i) "/" iterations)
        (println)
        (System/gc)

        (do-timing m "Depth of Inheritance Tree:"
                   classes-by-depth-of-inheritance-tree
                   classes-by-depth-of-inheritance-tree-forkjoin)

        (do-timing m "Coupling between Objects:"
                   classes-by-coupling-between-objects
                   classes-by-coupling-between-objects-forkjoin)

        (do-timing m "Weighted Methods per Class:"
                   classes-by-weighted-methods-per-class
                   classes-by-weighted-methods-per-class-forkjoin)

        (do-timing m "Number of Children:"
                   classes-by-number-of-children
                   classes-by-number-of-children-forkjoin)

        (do-timing m "Response for a Class:"
                   classes-by-response-for-a-class
                   classes-by-response-for-a-class-forkjoin)

        (do-timing m "Lack of Cohesion in Methods:"
                   classes-by-lack-of-cohesion-in-methods
                   classes-by-lack-of-cohesion-in-methods-forkjoin)))))

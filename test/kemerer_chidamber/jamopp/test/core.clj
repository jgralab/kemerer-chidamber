(ns kemerer-chidamber.jamopp.test.core
  (:use [kemerer-chidamber.jamopp.core])
  (:use [funnyqt.emf.core])
  (:use [funnyqt.emf.query])
  (:use [funnyqt.generic])
  (:use kemerer-chidamber.test.generic)
  (:use [clojure.test])
  (:require [clojure.java.io :as io]))

(defn register-jamopp-metamodel
  [ecore-file]
  (load-metamodel ecore-file))

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
  (when-not (.exists (io/file "jgralab.xmi"))
    (gunzip "jgralab.xmi.gz" "jgralab.xmi"))
  (load-model "jgralab.xmi"))

(def jm (memoize load-jgralab-model))

(deftest test-concrete-classifier-by-name
  ;; Simple name
  (is (concrete-classifier-by-name (jm) 'Vertex))
  ;; Qualified name
  (is (concrete-classifier-by-name (jm) 'de.uni_koblenz.jgralab.Vertex))
  (is (thrown-with-msg? RuntimeException
        #".* is ambiguous\."
        (concrete-classifier-by-name (jm) 'VertexImpl)))
  (is (thrown-with-msg? RuntimeException
        #"No such ConcreteClassifier .*"
        (concrete-classifier-by-name (jm) 'de.uni_koblenz.foo.Bar))))

(deftest benchmark
  (println "Running with a JaMoPP Model")
  (println "===========================")
  (let [m (jm)
        iterations 2]
      (dotimes [i iterations]
        (println)
        (println "Run" (inc i) "/" iterations)
        (println)
        (System/gc)

        (do-timing m "Depth of Inheritance Tree:"
                   classes-by-depth-of-inheritance-tree
                   classes-by-depth-of-inheritance-tree-forkjoin))))

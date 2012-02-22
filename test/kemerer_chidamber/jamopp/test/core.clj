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
    (println "Gunzipping jgralab.xmi.gz...")
    (gunzip "jgralab.xmi.gz" "jgralab.xmi"))
  (println "Loading JaMoPP model of jgralab...")
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
  ;; Restrict to only those uris for a slightly faster EClass lookup
  (with-ns-uris ["http://www.emftext.org/java/arrays"
                 "http://www.emftext.org/java/instantiations"
                 "http://www.emftext.org/java/parameters"
                 "http://www.emftext.org/java/commons"
                 "http://www.emftext.org/java/statements"
                 "http://www.emftext.org/java/members"
                 "http://www.emftext.org/java/classifiers"
                 "http://www.emftext.org/java/generics"
                 "http://www.emftext.org/java/modifiers"
                 "http://www.emftext.org/java/containers"
                 "http://www.emftext.org/java/imports"
                 "http://www.emftext.org/java/literals"
                 "http://www.emftext.org/java/references"
                 "http://www.emftext.org/java/operators"
                 "http://www.emftext.org/java/expressions"
                 "http://www.emftext.org/java/types"
                 "http://www.emftext.org/java/annotations"
                 "http://www.emftext.org/java/variables"]
    (let [m (jm)
          iterations 2]
      (binding [*get-classes-fn* (fn [m]
                                   (filter
                                    (fn [c]
                                      (re-matches #"de\.uni_koblenz\.jgralab\..*"
                                                  (class-qname c)))
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
                     classes-by-lack-of-cohesion-in-methods-forkjoin))))))

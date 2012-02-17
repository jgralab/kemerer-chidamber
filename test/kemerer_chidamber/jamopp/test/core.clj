(ns kemerer-chidamber.jamopp.test.core
  (:use [kemerer-chidamber.jamopp.core])
  (:use [funnyqt.emf.core])
  (:use [funnyqt.emf.query])
  (:use [funnyqt.generic])
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

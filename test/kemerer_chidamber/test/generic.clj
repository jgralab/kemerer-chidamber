(ns kemerer-chidamber.test.generic
  (:use [clojure.test]))

(defn do-timing [g title sv fjv]
  (println " " title)
  (print "    - Sequential: ")
  (let [r1 (time (doall (sv g)))]
    (print "    - ForkJoin:   ")
    (let [r2 (time (doall (fjv g)))]
      (is (= r1 r2)))))

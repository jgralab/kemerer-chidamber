(defproject kemerer-chidamber "1.1.0"
  :description "The Kemerer & Chidamber metrics for GraBaJa & JaMoPP syntax graphs."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [funnyqt "0.2.2"]]
  :warn-on-reflection true
  :main kemerer-chidamber.main
  :jvm-opts ["-Xmx2G"])

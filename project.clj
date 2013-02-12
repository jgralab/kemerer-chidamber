(defproject kemerer-chidamber "1.2.0"
  :description "The Kemerer & Chidamber metrics for GraBaJa & JaMoPP syntax graphs."
  :dependencies [[org.clojure/clojure "1.5.0-RC15"]
                 [funnyqt "0.4.13"]]
  :license {:name "GNU General Public License, Version 3"
            :url "http://www.gnu.org/copyleft/gpl.html"
            :distribution :repo}
  :url "https://github.com/jgralab/kemerer-chidamber"
  :warn-on-reflection true
  :jar-exclusions [#"(?:^|/).(svn|hg|git|tg|tg\.gz|xmi|xmi\.gz)/"]
  :main ^:skip-aot kemerer-chidamber.main
  :jvm-opts ["-Xmx2G"])

(defproject kemerer-chidamber "1.1.1"
  :description "The Kemerer & Chidamber metrics for GraBaJa & JaMoPP syntax graphs."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [funnyqt "0.2.5"]]
  :license {:name "GNU General Public License, Version 3"
            :url "http://www.gnu.org/copyleft/gpl.html"
            :distribution :repo}
  :url "https://github.com/jgralab/kemerer-chidamber"
  :warn-on-reflection true
  :jar-exclusions [#"(?:^|/).(svn|hg|git|tg|tg\.gz|xmi|xmi\.gz)/"]
  :main kemerer-chidamber.main
  :jvm-opts ["-Xmx2G"])

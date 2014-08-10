(defproject nxbrowse "0.1.1-SNAPSHOT"
  :description "A browser for PKG4 NX files."
  :url "http://github.com/louispvb/"
  :license {:name "GNU General Public License v3"
            :url "http://www.gnu.org/licenses/gpl.txt"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [pkgnx/pkgnx "3.0.1"]
                 [seesaw "1.4.4"]
                 [org.clojure/tools.logging "0.3.0"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [com.github.insubstantial/substance "7.2.1"]
                 [com.github.insubstantial/trident "7.2.1"]
                 [org.jogamp.gluegen/gluegen-rt-main "2.2.0"]
                 [org.jogamp.jogl/jogl-all-main "2.2.0"]
                 [org.jogamp.joal/joal-main "2.2.0"]
                 [clj-yaml "0.4.0"]]
  :main nxbrowse.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot [nxbrowse.core]}})

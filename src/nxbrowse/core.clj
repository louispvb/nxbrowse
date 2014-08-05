(ns nxbrowse.core
  (:require [nxbrowse.gui.app :refer [open-app]]
            [clojure.tools.logging :as log])
  (:gen-class))

#_(native!)

;TODO: add trie autocompletion
;TODO: drag and drop bitmaps out of file and "save to file" feature
;TODO: hook up properties panel to audio and bitmap properties and texts
;TODO: Add autoplay configuration
;TODO: search by depth and path (default selected path in textbox grayed out) (default radiobutton search from root)

(defn -main
  [& args]
  (System/setProperty "apple.laf.useScreenMenuBar" "false")
  (log/infof "Starting nxbrowse")
  (try (open-app)
       (catch Exception e (log/warn "Unhandled exception: " (.getMessage e))))
  0)

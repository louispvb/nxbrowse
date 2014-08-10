(ns nxbrowse.core
  (:require [clojure.tools.logging :as log]
            [nxbrowse.gui.core :refer [open-gui]]
            [nxbrowse.gui.handlers :refer [hook-exit-thread!]]
            [nxbrowse.util :refer [load-config! app-config]])
  (:gen-class))

#_(native!)

;TODO: set exit_on_close, dispose shortcut for debug
;TODO: add trie autocompletion
;TODO: drag and drop bitmaps out of file and "save to file" feature
;TODO: hook up properties panel to audio and bitmap properties
;TODO: Add autoplay configuration
;TODO: search by depth and path (default selected path in textbox grayed out) (default radiobutton search from root)

(defn -main
  [& args]
  (System/setProperty "apple.laf.useScreenMenuBar" "false")
  (log/infof "Starting nxbrowse")
  (hook-exit-thread!)
  (load-config!)
  (try (open-gui (:theme @app-config))
       (catch Exception e (log/warn "Unhandled exception: " (.getMessage e)))))

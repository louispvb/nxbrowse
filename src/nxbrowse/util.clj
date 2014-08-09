(ns nxbrowse.util
  (:import (java.io File)
           (clojure.lang PersistentQueue))
  (:require [seesaw.core :refer [select]]
            [clj-yaml.core :as ym]
            [clojure.tools.logging :as log]
            [nxbrowse.gui.recently-opened :as recent]))

; Dirty dirty mutable state!
(def opened-nx-file (atom nil))
(def nxtree-table (atom nil))
(def root-frame (atom nil))
(def app-config (atom {:theme "Nimbus" :recently-opened []}))

(def config-path "nxbrowse.cfg.yml")

(defn load-config!
  []
  (let [file (File. config-path)]
    (when (and (.exists file) (not (.isDirectory file)))
      (log/debugf "Loading configuration from \"%s\"" (.getAbsolutePath file))
      (reset! app-config (ym/parse-string (slurp config-path)))
      (doseq [rp (:recently-opened @app-config)]
        (recent/add-recent-path! rp)))))

(defn save-config!
  []
  (log/debug "Saving recently opened vector " (recent/recently-opened-vector))
  (swap! app-config #(assoc % :recently-opened (recent/recently-opened-vector)))
  (spit config-path (ym/generate-string @app-config)))

(defn select-id
  "Select widget id from root frame"
  [widget-id]
  (select @root-frame [widget-id]))
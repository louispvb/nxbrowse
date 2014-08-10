(ns nxbrowse.util
  (:import (java.io File)
           (clojure.lang PersistentQueue))
  (:require [seesaw.core :refer [select]]
            [clj-yaml.core :as ym]
            [clojure.tools.logging :as log]
            [nxbrowse.gui.recently-opened :as recent]))

(def opened-nx-file (atom nil))
(def root-frame (atom nil))
(def app-config (atom {:theme "Nimbus" :recently-opened []}))

(def config-path "nxbrowse.cfg.yml")

(defn load-config!
  []
  (let [file (File. config-path)
        _ (when (and (.exists file) (not (.isDirectory file)))
            (log/debugf "Loading configuration from \"%s\""
                        (.getAbsolutePath file))
            (reset! app-config (ym/parse-string (slurp config-path)))
            (doseq [rp (:recently-opened @app-config)]
              (recent/add-recent-path! rp)))]
    nil))

(defn save-config!
  []
  (let [_ (log/debug "Saving recently opened vector "
                     (recent/recently-opened-vector))
        _ (swap! app-config
                 #(assoc % :recently-opened (recent/recently-opened-vector)))
        _ (spit config-path (ym/generate-string @app-config))]
    nil))

(defn select-vec
  "Select with key vector from root frame."
  [key-vec]
  (select @root-frame key-vec))

(defn select-key
  "Select key from root frame"
  [key-id]
  (let [sel (select-vec [key-id])]
    (if (seq? sel) (first sel) sel)))
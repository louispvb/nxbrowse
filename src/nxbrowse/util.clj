(ns nxbrowse.util
  (:require [seesaw.core :refer [select]]
    ))

; Dirty dirty mutable state!
(def opened-nx-file (atom nil))
(def nxtree-table (atom nil))
(def root-frame (atom nil))
(def border-color (atom nil))

(defn select-id
  "Select widget id from root frame"
  [widget-id]
  (select @root-frame [widget-id]))
(ns nxbrowse.gui.handlers
  (:import (us.aaronweiss.pkgnx.internal NXHeader)
           (us.aaronweiss.pkgnx NXException LazyNXFile NXNode)
           (javax.swing.tree TreePath)
           (java.awt.event KeyEvent)
           (javax.swing.event TreeSelectionListener))
  (:require [clojure.tools.logging :as log]
            [seesaw.core :refer :all]
            [seesaw.table :refer [clear!
                                  insert-at!]]
            [seesaw.chooser :refer [choose-file]]
            [nxbrowse.nxfuns :refer [nx-attach-meta
                                     nx-property-map]]
            [nxbrowse.util :refer :all]
            [nxbrowse.gui.tree-table :refer [create-tree-table
                                             scroll-to-path]]))

;TODO: audio and bitmap selections
(defn create-tree-selection-listener
  []
  (reify
    TreeSelectionListener
    (valueChanged [this e]
      (let []
        )
      (let [path (.getPath e)
            props-table (select-id :#properties)
            node (.getLastPathComponent path)
            props (nx-property-map (nx-attach-meta node))]
        (text! (select-id :#tree-path)
               (clojure.string/join "/" (.getPath path)))
        (clear! props-table)
        (doseq [[row-num row] (map-indexed list (reverse props))]
          (insert-at! props-table row-num
                      {:property (nth row 0) :value (nth row 1)}))))))

(defn open-nx-file [file]
  "Initializes GUI components when nx file is opened. Sets opened-nx-file and
  nxtree-table atoms."
  (try (when file
         (reset! opened-nx-file (LazyNXFile. (.getAbsolutePath file)))
         (reset! nxtree-table (create-tree-table (.getRoot @opened-nx-file)))
         (let [header (.getHeader @opened-nx-file)]
           (log/debug
             "NXHeader Counts below"
             "\nTotal Node Count: " (.getNodeCount header)
             "\n    Bitmap Count: " (.getBitmapCount header)
             "\n    String Count: " (.getStringCount header)
             "\n     Sound Count: " (.getSoundCount header)))
         (config!
           (select-id :#tree-panel)
           :items [[(scrollable @nxtree-table)
                    "dock center"]])
         (config!
           (select-id :#node-count)
           :text (format "Node Count: %d"
                         (.getNodeCount (.getHeader @opened-nx-file))))
         (.addTreeSelectionListener @nxtree-table (create-tree-selection-listener)))
       (catch NXException e (log/warnf "Couldn't open invalid file \"%s\""
                                      (.getAbsolutePath file)))))

(defn file-open-handler [_]
  (open-nx-file (choose-file :type :open
                             :dir "."
                             :multi? false
                             :selection-mode :files-only
                             :filters [["NX Files" ["nx"]]])))

(defn navigate-path-handler [_]
  (when @nxtree-table
    (let [user-path (value (select-id :#tree-path))
          split-path (rest (clojure.string/split user-path #"/"))
          node-path (reduce
                      (fn [acc s]
                        (conj acc (.getChild (last acc) s)))
                      [(.getRoot @opened-nx-file)]
                      split-path)
          tree-path (TreePath. (to-array node-path))]
     (scroll-to-path @nxtree-table tree-path) )))
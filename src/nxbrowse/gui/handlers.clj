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

(defn view-node-dispatch
  [{:keys [type data-get]}]
  (let [view (select-id :#view-panel)]
    (.removeAll view)
    (case type
      :string (.add view (scrollable (text :text @data-get
                                           :multi-line? true
                                           :editable? false
                                           :wrap-lines? true
                                           :margin 10)))
      nil)
    (.validate view)
    (.repaint view 50)))

;TODO: audio and bitmap selections
(defn create-tree-selection-listener
  []
  (reify
    TreeSelectionListener
    (valueChanged [this e]
      (let [path (.getPath e)
            props-table (select-id :#properties)
            node (.getLastPathComponent path)
            props (nx-property-map (nx-attach-meta node))]
        ; Set gui path
        (text! (select-id :#tree-path)
               (clojure.string/join "/" (.getPath path)))
        ; Fill properties table
        (clear! props-table)
        (doseq [[row-num [k v]] (map-indexed list props)]
          (insert-at! props-table row-num {:property k :value v}))
        ; View node
        (view-node-dispatch (nx-attach-meta node))))))

(defn show-header-info [_]
  (if @opened-nx-file
    (let [h (.getHeader @opened-nx-file)
          t (table :preferred-size [300 :by 200]
                   :model [:columns [:name :value]])
          info (array-map "Total Node Count" (.getNodeCount h)
                          "Bitmap Count" (.getBitmapCount h)
                          "Sound Count" (.getSoundCount h)
                          "String Count" (.getStringCount h)
                          "Node Offset" (.getNodeOffset h)
                          "Bitmap Offset" (.getBitmapOffset h)
                          "Sound Offset" (.getSoundOffset h)
                          "String Offset" (.getStringOffset h))
          f (frame :title "NX File Header"
                   :content t
                   :on-close :dispose)]
      (doseq [[row-num [k v]] (map-indexed list info)]
        (insert-at! t row-num {:name k :value v}))
      (-> f (pack!) (show!) (.setLocationRelativeTo nil)))
    (alert "A file must be opened to view header information.")))

(defn open-nx-file [file]
  "Initializes GUI components when nx file is opened. Sets opened-nx-file and
  nxtree-table atoms."
  (try (when file
         (reset! opened-nx-file (LazyNXFile. (.getAbsolutePath file)))
         (reset! nxtree-table (create-tree-table (.getRoot @opened-nx-file)))
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
     (scroll-to-path @nxtree-table tree-path))))
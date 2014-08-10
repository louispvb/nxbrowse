(ns nxbrowse.gui.handlers
  (:import (us.aaronweiss.pkgnx.internal NXHeader)
           (us.aaronweiss.pkgnx NXException LazyNXFile NXNode)
           (javax.swing.tree TreePath)
           (javax.swing.event TreeSelectionListener)
           (java.lang Runtime)
           (java.nio.file NoSuchFileException))
  (:require [clojure.tools.logging :as log]
            [seesaw.core :refer :all]
            [seesaw.keymap :refer [map-key]]
            [seesaw.table :refer [clear!
                                  insert-at!]]
            [seesaw.chooser :refer [choose-file]]
            [nxbrowse.nx-data :refer [nx-attach-meta
                                     nx-property-map]]
            [nxbrowse.util :refer :all]
            [nxbrowse.gui.recently-opened :as recent]
            [nxbrowse.gui.tree-table :refer [create-tree-table
                                             scroll-to-path
                                             toggle-tree-sel]]))

(def system-exit-thread
  (Thread. (fn []
             (log/info "Stopping nxbrowse.")
             (save-config!)
             (invoke-now (.setVisible @root-frame false)
                         (.dispose @root-frame))
             (System/exit 0))))

(defn view-node-dispatch
  [{:keys [type data-get]}]
  (let [view (select-key :#view-panel)]
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
            props-table (select-key :#properties)
            node (.getLastPathComponent path)
            props (nx-property-map (nx-attach-meta node))]
        ; Set gui path
        (text! (select-key :#tree-path)
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
          info (array-map "Total Node Count" (.getNodeCount h)
                          "Bitmap Count" (.getBitmapCount h)
                          "Sound Count" (.getSoundCount h)
                          "String Count" (.getStringCount h)
                          "Node Offset" (.getNodeOffset h)
                          "Bitmap Offset" (.getBitmapOffset h)
                          "Sound Offset" (.getSoundOffset h)
                          "String Offset" (.getStringOffset h))
          _ (clear! (select-key :#properties))
          _ (doseq [[row-num [k v]] (map-indexed list info)]
              (insert-at! (select-key :#properties)
                          row-num {:property k :value v}))]
      nil)
    (alert "A file must be opened to view header information.")))

(defn open-nx-file
  "Initializes GUI components when nx file is opened. Sets opened-nx-file and
  nxtree-table atoms."
  [file]
  (try (when-let [file (if (string? file) (LazyNXFile. file) file)]
         (reset! opened-nx-file file)
         ; Add tree table
         (config!
           (select-key :#tree-panel)
           :items [[(scrollable (create-tree-table (.getRoot @opened-nx-file)))
                    "dock center"]])

         ; Display node count
         (config!
           (select-key :#node-count)
           :text (format "Node Count: %d"
                         (.getNodeCount (.getHeader @opened-nx-file))))

         ; Listeners
         (let [treetable (select-key :JXTreeTable)]
           (.addTreeSelectionListener treetable
                                      (create-tree-selection-listener))
           (letfn [(f [_] (toggle-tree-sel (select-key :JXTreeTable)))]
             (map-key treetable "ENTER" f)
             (map-key treetable "SPACE" f)))
         ; Add recently opened to config
         (recent/add-recent-path! (.getFilePath file))
         (recent/update-recent-menu! @root-frame open-nx-file))
       (catch NoSuchFileException e
         (alert "Could not open file due to requested file not found."))))

(defn file-open-handler [_]
  (let [file-choice (choose-file :type :open
                                 :dir "."
                                 :multi? false
                                 :selection-mode :files-only
                                 :filters [["NX Files" ["nx"]]])]
    (when file-choice
      (try (open-nx-file (LazyNXFile. (.getAbsolutePath file-choice)))
           (catch NXException e
             (let [fe (format "%s\non file \"%s\""
                              (.getMessage e)
                              (.getAbsolutePath file-choice))]
               (alert fe) (log/warn fe)))))))

(defn navigate-path-handler [_]
  (when (select-key :JXTreeTable)
    (let [user-path (value (select-key :#tree-path))
          split-path (rest (clojure.string/split user-path #"/"))
          node-path (reduce
                      (fn [acc s]
                        (conj acc (.getChild (last acc) s)))
                      [(.getRoot @opened-nx-file)]
                      split-path)
          tree-path (TreePath. (to-array node-path))
          _ (scroll-to-path (select-key :JXTreeTable) tree-path)]
     nil)))

(defn most-recent-handler [_]
  (if-let [p (first (recent/recently-opened-vector))]
    (open-nx-file p)
    (alert "There are no recent files to open.")))

(defn hook-exit-thread!
  []
  (log/debug "Hooking exit thread..")
  (-> (Runtime/getRuntime)
      (.addShutdownHook system-exit-thread)))
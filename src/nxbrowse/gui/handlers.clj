(ns nxbrowse.gui.handlers
  (:import (us.aaronweiss.pkgnx.internal NXHeader)
           (us.aaronweiss.pkgnx NXException LazyNXFile NXNode)
           (javax.swing.tree TreePath)
           (java.awt.event KeyEvent)
           (javax.swing.event TreeSelectionListener)
           (java.lang Runtime))
  (:require [clojure.tools.logging :as log]
            [seesaw.core :refer :all]
            [seesaw.keymap :refer [map-key]]
            [seesaw.table :refer [clear!
                                  insert-at!]]
            [seesaw.chooser :refer [choose-file]]
            [nxbrowse.nxfuns :refer [nx-attach-meta
                                     nx-property-map]]
            [nxbrowse.util :refer :all]
            [nxbrowse.gui.recently-opened :as recent]
            [nxbrowse.gui.tree-table :refer [create-tree-table
                                             scroll-to-path
                                             toggle-tree-sel]]))

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

(defn open-nx-file
  "Initializes GUI components when nx file is opened. Sets opened-nx-file and
  nxtree-table atoms."
  [file]
  (when-let [file (if (string? file) (LazyNXFile. file) file)]
    (reset! opened-nx-file file)
    (reset! nxtree-table (create-tree-table (.getRoot @opened-nx-file)))
    ; Add tree table
    (config!
      (select-id :#tree-panel)
      :items [[(scrollable @nxtree-table)
               "dock center"]])

    ; Display node count
    (config!
      (select-id :#node-count)
      :text (format "Node Count: %d"
                    (.getNodeCount (.getHeader @opened-nx-file))))

    ; Listeners
    (.addTreeSelectionListener @nxtree-table
                               (create-tree-selection-listener))
    (letfn [(f [_] (toggle-tree-sel @nxtree-table))]
      (map-key @nxtree-table "ENTER" f)
      (map-key @nxtree-table "SPACE" f))
    ; Add recently opened to config
    (recent/add-recent-path! (.getFilePath file))
    (recent/update-recent-menu! @root-frame open-nx-file)))

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

(defn most-recent-handler [_]
  (if-let [p (first (recent/recently-opened-vector))]
    (open-nx-file p)
    (alert "There are no recent files to open.")))

(def system-exit-thread
  (Thread. (fn []
             (log/info "Stopping nxbrowse.")
             (save-config!)
             (invoke-now (.setVisible @root-frame false)
                         (.dispose @root-frame))
             (System/exit 0))))

(defn hook-exit-thread!
  []
  (log/debug "Hooking exit thread..")
  (-> (Runtime/getRuntime)
      (.addShutdownHook system-exit-thread)))
(ns nxbrowse.gui.tree-table
  (:import (org.jdesktop.swingx.treetable AbstractTreeTableModel)
           (org.jdesktop.swingx JXTreeTable)
           (us.aaronweiss.pkgnx NXNode))
  (:require [clojure.tools.logging :as log]
            [seesaw.core :refer :all]
            [nxbrowse.util :refer [root-frame]]
            [nxbrowse.nx-data :refer :all]))

(defn toggle-tree-sel
  "Toggles currently selected path."
  [tree]
  (let [sel-path (-> tree (.getTreeSelectionModel) (.getSelectionPath))
        _ (if (.isExpanded tree sel-path)
            (.collapsePath tree sel-path)
            (.expandPath tree sel-path))]))

(defn scroll-to-path
  "Scrolls, selects, and expands tree table to path."
  [tree-table path]
  (let [select-model (.getTreeSelectionModel tree-table)]
    (.clearSelection select-model)
    (.setSelectionPath select-model path)
    (doto tree-table
      (.expandPath path)
      (.scrollRectToVisible
        (.getCellRect tree-table (.getRowForPath tree-table path) 0 true)))))

(defn create-nxtreetable-model
  [root]
  (let [columns ["Name" "Type" "Data"]]
    (proxy [AbstractTreeTableModel]
           [root]
      (getColumnCount [] (count columns))
      (getColumnName [column-num] (nth columns column-num ""))
      (getChildCount [parent] (.getChildCount parent))
      (getValueAt
        [node column-num]
        (let [meta (nx-attach-meta node)]
        (case column-num
          0 (.getName node)
          1 (:name meta)
          2 (nx-data-text-simple meta)
          "")))
      (getIndexOfChild
        [parent child]
        (.indexOf (vec parent) child))
      (getChild
        [parent index]
        (nth (seq parent) index nil))
      (isLeaf [node]
        (= (.getChildCount node) 0)))))

(defn create-tree-table [root]
  (let [tree-table (JXTreeTable. (create-nxtreetable-model root))
        column-model (.getColumnModel tree-table)
        resize-column #(.setPreferredWidth (.getColumn column-model %1) %2)
        width 200]
    (resize-column 0 width)
    (resize-column 1 50)
    (resize-column 2 width)
    (.setScrollsOnExpand tree-table true)
    tree-table))

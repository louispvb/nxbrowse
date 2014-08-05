(ns nxbrowse.gui.tree
  (:require [clojure.tools.logging :as log])
  (:import (us.aaronweiss.pkgnx NXNode)
           (javax.swing JTree)
           (javax.swing.tree DefaultMutableTreeNode TreePath)
           (javax.swing.event TreeWillExpandListener)))

#_(defrecord NXTreeModel [listeners root]
  TreeModel
  (addTreeModelListener [_ l]
    (conj listeners l)
    nil)
  (removeTreeModelListeners [_ l]
    (disj listeners l))
  (getRoot [_] root)
  (getChildCount [_] (.getChildCount root))
  (isLeaf [_] (= (.getChildCount root) 0)))

(defn populate-children [parent]
  "Takes a parent DefaultMutableNode which must contain an inner NXNode and
   populates it with its children."
  (doseq [nx (.getUserObject parent)]
    (let [child (DefaultMutableTreeNode. nx)]
      (.add parent child)
      (when (> (.getChildCount nx) 0)
        (.add child (DefaultMutableTreeNode. "Loading...")))))
  (.remove parent 0))

(defrecord Will-Expand-Listener []
  TreeWillExpandListener
  (treeWillExpand [_ e]
    (let [parent (-> e (.getPath) (.getLastPathComponent))]
      (populate-children parent)))
  (treeWillCollapse [_ _] nil))

(defn create-tree [root]
  (let [boxed-root (DefaultMutableTreeNode. root)
        _ (populate-children boxed-root)
        ;_ (.add boxed-root (DefaultMutableTreeNode. "Loading..."))
        tree (JTree. boxed-root)]
    (.setRootVisible tree false)
    (.addTreeWillExpandListener tree (Will-Expand-Listener.))
    tree))
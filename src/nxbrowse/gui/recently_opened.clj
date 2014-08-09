(ns nxbrowse.gui.recently-opened
  (:import (clojure.lang PersistentQueue)
           (us.aaronweiss.pkgnx LazyNXFile))
  (:require [seesaw.core :refer :all]))

(def recent-queue (atom PersistentQueue/EMPTY))

(defn- conj-limit
  "Conj onto collection and pops one if new collection size is greater than
  limit."
  [col item limit]
  (let [conjed (conj col item)]
    (if (> (count conjed) limit) (pop conjed) conjed)))

(defn add-recent-path!
  "Adds a recent path to config unless it already exists in it."
  [str-path]
  (when (empty? (filter #(= % str-path) @recent-queue))
    (swap! recent-queue conj-limit str-path 5)))

(defn recently-opened-vector
  "Get recently opened vector."
  []
  (vec @recent-queue))

(defn- get-open-actions
  "Vector of open actions."
  [handler]
  (map #(action :name (last (clojure.string/split % #"/"))
                :handler (fn [_] (handler %)))
       (recently-opened-vector)))

(defn update-recent-menu!
  "Update recent menu's list of actions. Takes a handler to open files that must
  accept one absolute path argument."
  [select-from open-handler]
  (let [menu (select select-from [:#recent-menu])]
    (.removeAll menu)
    (doseq [a (get-open-actions open-handler)]
      (.add menu a))))

(defn recent-open-menu
  "Create a recent open menu. Takes a handler to open files that must accept one
  absolute string path argument."
  [& {:keys [name handler]}]
  (menu
    :id :recent-menu
    :text name
    :items (get-open-actions handler)))
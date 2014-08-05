(ns nxbrowse.nxfuns
  (:import (us.aaronweiss.pkgnx.nodes NXNullNode NXLongNode NXDoubleNode
                                      NXStringNode NXPointNode NXBitmapNode
                                      NXAudioNode))
  (:require [clojure.tools.logging :as log]))

(defn nx-data-meta
  "Get an NXNode's meta info."
  [nxnode]
  (cond
    (instance? NXNullNode nxnode)
    {:type :null,
     :name "",
     :id   0}
    (instance? NXLongNode nxnode)
    {:type     :long,
     :name     "integral",
     :id       1
     :data-get #(.getLong %)}
    (instance? NXDoubleNode nxnode)
    {:type     :double,
     :name     "floating",
     :id       2
     :data-get #(.getDouble %)}
    (instance? NXStringNode nxnode)
    {:type     :string,
     :name     "text",
     :id       3
     :data-get #(.getString %)}
    (instance? NXPointNode nxnode)
    {:type     :point,
     :name     "point",
     :id       4
     :data-get (fn [n] (let [p (.getPoint n)]
                         (format "[x:%d, y:%d]" (.x p) (.y p))))}
    (instance? NXBitmapNode nxnode)
    {:type :bitmap,
     :name "bitmap",
     :id   5}
    (instance? NXAudioNode nxnode)
    {:type :audio,
     :name "audio",
     :id   6}
    :else (do (log/warnf "Can't determine node type of \"%s\"."
                         (.getName nxnode))
              {:type :null
               :name "INVALID NODE"
               :id -1})))

(defn nx-data-text
  "Returns an NXNode's representation as text only if it can be displayed simply
  as so (no binary data)."
  [nxnode]
  (let [meta (nx-data-meta nxnode)]
    (if (contains? meta :data-get)
      (str ((:data-get meta) nxnode))
      "")))
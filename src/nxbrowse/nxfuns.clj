(ns nxbrowse.nxfuns
  (:import (us.aaronweiss.pkgnx.nodes NXNullNode NXLongNode NXDoubleNode
                                      NXStringNode NXPointNode NXBitmapNode
                                      NXAudioNode)
           java.awt.Point)
  (:require [clojure.tools.logging :as log]))

(defn nx-attach-meta
  "Attaches some meta data to a node for easier processing."
  [nxnode]
  (conj (cond
          (instance? NXNullNode nxnode)
          {:type     :null
           :name     ""
           :data-get (constantly nil)}
          (instance? NXLongNode nxnode)
          {:type     :long
           :name     "integral"
           :data-get (fn [] (.getLong nxnode))}
          (instance? NXDoubleNode nxnode)
          {:type     :double
           :name     "floating"
           :data-get (fn [] (.getDouble nxnode))}
          (instance? NXStringNode nxnode)
          {:type     :string
           :name     "text"
           :data-get (fn [] (.getString nxnode))}
          (instance? NXPointNode nxnode)
          {:type     :point
           :name     "point"
           :data-get (fn [] (.getPoint nxnode))}
          (instance? NXBitmapNode nxnode)
          {:type     :bitmap
           :name     "bitmap"
           :data-get (fn [] (.getImage nxnode))}
          (instance? NXAudioNode nxnode)
          {:type     :audio
           :name     "audio"
           :data-get (fn [] (.getAudioBuf nxnode))}
          :else (do (log/warnf "Exception: Can't determine node type of \"%s\"."
                               (.getName nxnode))
                    {:type :null
                     :name "INVALID NODE"
                     :data (constantly nil)}))
        {:node nxnode}))

;TODO bitmap resolution
;TODO audio length, type

(defn nx-property-map
  "Get an NXNode's map of properties."
  [{:keys [node data-get type]}]
  #_{"test" "tesst"}
  (conj
    {"Child Index" (.getFirstChildIndex node)
     "Child Count" (.getChildCount node)}
    (case type
          :long {"Integral Data" (data-get)}
          :double {"Floating Data" (data-get)}
          :string {"Text Length" (.length (data-get))}
          :bitmap {"Horizontal Res"  "?"
                   "Vertical Res"    "?"
                   "Data Size (KiB)" "?"}
          :audio {"Audio Length"    "?"
                  "Format"          "?"
                  "Data Size (KiB)" "?"}
          {})))

(defn nx-data-text-simple
  "Returns an NXNode's representation as text only if it can be displayed simply
  as so (no binary data)."
  [{:keys [type data-get]}]
  (if (contains? #{:long :double :string :point} type)
    (if (= type :point)
      (let [p (data-get)] (format "[x:%s, y:%s]" (.x p) (.y p)))
      (data-get))
    ""))
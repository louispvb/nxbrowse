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
           :data-get (delay nil)}
          (instance? NXLongNode nxnode)
          {:type     :long
           :name     "integral"
           :data-get (delay (.getLong nxnode))}
          (instance? NXDoubleNode nxnode)
          {:type     :double
           :name     "floating"
           :data-get (delay (.getDouble nxnode))}
          (instance? NXStringNode nxnode)
          {:type     :string
           :name     "text"
           :data-get (delay (.getString nxnode))}
          (instance? NXPointNode nxnode)
          {:type     :point
           :name     "point"
           :data-get (delay (.getPoint nxnode))}
          (instance? NXBitmapNode nxnode)
          {:type     :bitmap
           :name     "bitmap"
           :data-get (delay (.getImage nxnode))}
          (instance? NXAudioNode nxnode)
          {:type     :audio
           :name     "audio"
           :data-get (delay (.getAudioBuf nxnode))}
          :else (do (log/warnf "Exception: Can't determine node type of \"%s\"."
                               (.getName nxnode))
                    {:type :null
                     :name "INVALID NODE"
                     :data-get (delay nil)}))
        {:node nxnode}))

;TODO bitmap resolution
;TODO audio length, type

(defn nx-property-map
  "Get an NXNode's map of properties."
  [{:keys [node data-get type]}]
  (conj
    (array-map "Child Count" (.getChildCount node)
               "Child Index" (.getFirstChildIndex node))
    (reverse (case type
               :long (array-map "Integral Data" @data-get)
               :double (array-map "Floating Data" @data-get)
               :string (array-map "Text Length" (.length @data-get))
               :point (array-map "x" (.x @data-get)
                                 "y" (.y @data-get))
               :bitmap (array-map "Horizontal Res" (.getWidth @data-get)
                                  "Vertical Res" (.getHeight @data-get)
                                  "Data Size (KiB)" "?")
               :audio (array-map "Audio Length" "?"
                                 "Format" "?"
                                 "Data Size (KiB)" "?")
               (array-map)))))

(defn nx-data-text-simple
  "Returns an NXNode's representation as text only if it can be displayed simply
  as so (no binary data)."
  [{:keys [type data-get]}]
  (if (contains? #{:long :double :string :point} type)
    (if (= type :point)
      (format "[x:%5d, y:%5d]" (.x @data-get) (.y @data-get))
      @data-get)
    ""))
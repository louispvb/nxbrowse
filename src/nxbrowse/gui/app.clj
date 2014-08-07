(ns nxbrowse.gui.app
  (:import (javax.swing UIManager JFrame JDialog InputMap SwingUtilities)
           (javax.swing.text DefaultEditorKit))
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.keystroke :refer [keystroke]]
            [seesaw.keymap :refer [map-key]]
            [seesaw.mig :refer [mig-panel]]
            [clojure.tools.logging :as log]
            [nxbrowse.gui.handlers :refer :all]
            [nxbrowse.util :refer [root-frame
                                   border-color]]))

(defn init-gui
  "Instantiates all components of main window. Returns the root frame."
  []
  (let [border-color
        (.. (UIManager/getDefaults)
            (getColor "Label.background")
            (darker))

        tree-panel (mig-panel :id :tree-panel
                              :constraints ["insets 0"] :items [])
        view-panel (flow-panel :items [(label "view")])
        properties-panel
        (scrollable (table
                      :id :properties
                      :show-grid? false
                      :model [:columns [:property :value]
                              :rows []]))

        file-menu [(action :name "Open" :tip "Open an nx file"
                           :key "menu O" :handler file-open-handler)]
                                                            ; TODO: ENTER key goes, select all text on focus
        path-bar
        (mig-panel
          :constraints ["fill" "5[][]5" "2[]2"]
          :items [[(text :id :tree-path :columns 25) "dock center"]
                  [(button :text "Go"
                           :listen [:action navigate-path-handler]) ""]]
          :border (line-border :bottom 1 :color border-color))
                                                            ; TODO: Add progress bar on left that shows only when loading
        info-bar
        (mig-panel
          :constraints ["fill" "5[][]5" "2[]2"]
          :items [[(label :id :node-count :text "") ""]
                  [(label "PROGRESSBAR") "align right"]]
          :border (line-border :top 1 :color border-color))
        menu-bar (menubar :items [(menu :text "File" :items file-menu)])

        content-panel
        (mig-panel
          :constraints ["" "" ""]
          :items [[(left-right-split
                     tree-panel
                     (top-bottom-split
                       view-panel
                       properties-panel
                       :divider-location 7/10)
                     :divider-location 3/5)
                   "dock center"]
                  [info-bar "dock south"]
                  [path-bar "dock north"]])

        gui
        (frame :title "NXBrowse" :menubar menu-bar
               :size [1000 :by 600] :content content-panel)]
    (let [tree-path (select gui [:#tree-path])]
      (listen tree-path :focus-gained
              (fn [_] (.select tree-path 0 (.. tree-path (getText) (length))))))
    (map-key path-bar "ENTER" navigate-path-handler)
    gui))

(defn center!
  "Sets frame to center of the screen."
  [frame]
  (.setLocationRelativeTo frame nil)
  frame)

(defn force-top!
  "Forces frame to always be on top."
  [frame]
  (.setAlwaysOnTop frame true)
  frame)

(defn get-system-lafs
  "Returns a seq of system LAFs."
  []
  (seq (UIManager/getInstalledLookAndFeels)))

(defn set-osx-shortcuts
  []
  (let [input-map (UIManager/get "TextField.focusInputMap")
        mapping {"menu C" DefaultEditorKit/copyAction
                 "menu V" DefaultEditorKit/pasteAction
                 "menu X" DefaultEditorKit/cutAction
                 "menu A" DefaultEditorKit/selectAllAction}]
    (doseq [[k v] mapping]
      (.put input-map (keystroke k) v))))

(defn set-theme
  [theme-class-name]
  (try
    (UIManager/setLookAndFeel theme-class-name)
    (JFrame/setDefaultLookAndFeelDecorated false)
    (JDialog/setDefaultLookAndFeelDecorated false)
    (when @root-frame
      (SwingUtilities/updateComponentTreeUI @root-frame))
    (when (= (System/getProperty "os.name") "Mac OS X")
      (set-osx-shortcuts))
    (log/info "Current LAF: " (.getID (UIManager/getLookAndFeel)))
    (log/debug "Supported LAFs:")
    (doseq [laf (get-system-lafs)]
      (log/debug (.getName laf) ": " (.getClassName laf)))
    (catch Exception e (println "Could not set LAF: " (.getMessage e)))))

(defn open-app []
  (invoke-later
    (set-theme
      ;"org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel"
      ;"org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel"
      ;"org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel"
      ;(UIManager/getSystemLookAndFeelClassName)
      "javax.swing.plaf.nimbus.NimbusLookAndFeel"
      ;"javax.swing.plaf.metal.MetalLookAndFeel"
      ;"com.sun.java.swing.plaf.gtk.GTKLookAndFeel"
      ;"org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel"
      ;"org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel"
      ;GAGS
      ;"com.sun.java.swing.plaf.motif.MotifLookAndFeel"
      ;"org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel"
      )
    (reset! root-frame (init-gui))
    (-> @root-frame
      center!
      show!)

    ))
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
            [nxbrowse.util :refer [root-frame]]))

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

(defn init-gui
  "Instantiates all components of main window. Returns the root frame."
  []
  (let [border-color
        (.. (UIManager/getDefaults)
            (getColor "Label.background")
            (darker))

        lafs ["javax.swing.plaf.nimbus.NimbusLookAndFeel"
              "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel"
              (UIManager/getSystemLookAndFeelClassName)
              "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel"
              "javax.swing.plaf.metal.MetalLookAndFeel"
              "org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel"
              "org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel"
              ;Gags
              "com.sun.java.swing.plaf.motif.MotifLookAndFeel"
              "org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel"]

        tree-panel (mig-panel :id :tree-panel
                              :constraints ["insets 0"] :items [])
        view-panel (border-panel :id :view-panel)
        properties-panel
        (scrollable (table
                      :id :properties
                      :show-grid? false
                      :model [:columns [:property :value]
                              :rows []]))

        file-menu [(action :name "Open" :tip "Open an nx file"
                           :key "menu O" :handler file-open-handler)
                   :separator

                   (action :name "Quit"
                           :key "menu Q"
                           :handler (fn [e]
                                      (.dispose (to-frame e))
                                      (System/exit 0)))]
        ; TODO create new frame instead of updating current frame theme
        ; TODO make atom of currently selected theme and selected? for all
        view-menu [(action :name "Header Information"
                           :handler show-header-info)
                   :separator
                   (let [bg (button-group)]
                     (menu
                       :text "Select Theme"
                       :items [(radio-menu-item
                                 :text "Nimbus" :group bg :selected? true
                                 :listen [:action (fn [_]
                                                    (set-theme (lafs 0)))])
                               (radio-menu-item
                                 :text "Graphite" :group bg
                                 :listen [:action (fn [_]
                                                    (set-theme (lafs 1)))])
                               (radio-menu-item
                                 :text "System" :group bg
                                 :listen [:action (fn [_]
                                                    (set-theme (lafs 2)))])
                               (radio-menu-item
                                 :text "Silver Mist" :group bg
                                 :listen [:action (fn [_]
                                                    (set-theme (lafs 3)))])
                               (radio-menu-item
                                 :text "Metal" :group bg
                                 :listen [:action (fn [_]
                                                    (set-theme (lafs 4)))])
                               ]))]

        help-menu
        [(action :name "About"
                 :handler (fn [_]
                            (alert "NXBrowse\nBrowser for PKG4 NX file format\nhttp://github.com/louispvb/nxbrowse")))]
        path-bar
        (mig-panel
          :constraints ["fill" "5[][]5" "2[]2"]
          :items [[(text :id :tree-path :columns 25) "dock center"]
                  [(button :text "Go"
                           :listen [:action navigate-path-handler]) ""]]
          :border (line-border :bottom 1 :color border-color))
        info-bar
        (mig-panel
          :constraints ["fill" "5[][]5" "2[]2"]
          :items [[(label :id :node-count :text "") ""]]
          :border (line-border :top 1 :color border-color))

        menu-bar (menubar :items [(menu :text "File" :items file-menu)
                                  (menu :text "View" :items view-menu)
                                  (menu :text "Help" :items help-menu)])

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

(defn open-app []
  (invoke-later
    (set-theme "javax.swing.plaf.nimbus.NimbusLookAndFeel")
    (reset! root-frame (init-gui))
    (-> @root-frame
      center!
      show!)))
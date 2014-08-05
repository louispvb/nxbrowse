(ns nxbrowse.my-atoms)

; Dirty dirty mutable state!
(def opened-nx-file (atom nil))
(def nxtree-table (atom nil))
(def root-frame (atom nil))

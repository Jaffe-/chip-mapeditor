;; Chip's Challenge Map Editor
;;
;; User interface stuff

(ns mapeditor.ui
  (:use seesaw.core
        seesaw.graphics
        seesaw.color
        seesaw.dev
        mapeditor.graphics
        mapeditor.map))

(defn- get-block-image [level-data x y]
  ((nth blocks
        (nth level-data (+ (* y 32) x)))
   :image))

(defn- fill-map [c g]
  (let [{data :level-data :as level} (load-level "/Users/jaffe1/chip_ca65/src/level5.bin")]
    (dotimes [x 32]
      (dotimes [y 32]
        (paint-metatile g (get-block-image data x y) (* x 16) (* y 16))))))

(defn- paint-metatile [g image x y]
  (.drawImage g image x y nil))

(defn- paint-grid-metatile [g image grid-x grid-y]
  (paint-metatile g image (+ (* grid-x 21) 5) (+ (* grid-y 21) 5)))
  
(defn- fill-metatiles [g item-list getter-fn]
  (loop [[item & items] item-list x 0 y 0]
    (paint-grid-metatile g (getter-fn item) x y)
    (when-not (empty? items)
      (if (zero? (mod (inc x) 8))
        (recur items 0 (inc y))
        (recur items (inc x) y)))))

(defn- fill-blocks [c g]
  (fill-metatiles g blocks #(% :image)))

(defn- fill-objects [c g]
  (fill-metatiles g objects #((% :images) :up)))

(def map-canvas (canvas ;:background :black
                        :paint fill-map
                        :size [512 :by 512]))

(def block-canvas (canvas ;:background :black
                        :paint fill-blocks
                        :size [173 :by 152]))

(def object-canvas (canvas ;:background :black
                        :paint fill-objects
                        :size [173 :by 68]))

(defn make-window []
  (let [exit-action (action :handler dispose! :name "Exit")]
    (native!)
    (invoke-later
     (show!
      (frame :title "Chip's Challenge Map Editor"
             :size [840 :by 600]
             :content (border-panel
                     :border 5
                     :north (toolbar :items [exit-action])
                     :center (horizontal-panel
                              :border "Hehe"
                              :items [map-canvas]) 
                     :east (vertical-panel
                             :items [(horizontal-panel
                                      :border [5 "Blocks"]
                                      :items [block-canvas])
                                     (horizontal-panel
                                      :border [5 "Objects"]
                                      :items [object-canvas])
                                     (horizontal-panel
                                      :items [(button :text "Connect traps")
                                              (button :text "Some shit")])
                                     (horizontal-panel
                                      :items [(label :text "Number of chips")
                                              (text :enabled? nil)])
                                     (horizontal-panel
                                      :items [(label :text "Time")
                                              (text :enabled? nil)])]))
           :menubar (menubar :items [(menu :text "File" :items ["Load map" "Save map"])]))))))

(when-mouse-dragged)

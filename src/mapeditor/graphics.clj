;; Chip's Challenge Map Editor
;;
;; Handling of graphics (tiles, metatiles)

(ns mapeditor.graphics
  (:use [clojure.java.io :as io]
        [mapeditor.util]
        [seesaw.color])
  (:import [java.awt Graphics Image]
           [java.awt.image BufferedImage]))

(def palette-rgb-map
 (mapv #(apply color %)
       '[(0x80 0x80 0x80) (0x00 0x3D 0xA6) (0x00 0x12 0xB0) (0x44 0x00 0x96)
         (0xA1 0x00 0x5E) (0xC7 0x00 0x28) (0xBA 0x06 0x00) (0x8C 0x17 0x00)
         (0x5C 0x2F 0x00) (0x10 0x45 0x00) (0x05 0x4A 0x00) (0x00 0x47 0x2E)
         (0x00 0x41 0x66) (0x00 0x00 0x00) (0x05 0x05 0x05) (0x05 0x05 0x05)
         (0xC7 0xC7 0xC7) (0x00 0x77 0xFF) (0x21 0x55 0xFF) (0x82 0x37 0xFA)
         (0xEB 0x2F 0xB5) (0xFF 0x29 0x50) (0xFF 0x22 0x00) (0xD6 0x32 0x00)
         (0xC4 0x62 0x00) (0x35 0x80 0x00) (0x05 0x8F 0x00) (0x00 0x8A 0x55)
         (0x00 0x99 0xCC) (0x21 0x21 0x21) (0x09 0x09 0x09) (0x09 0x09 0x09)
         (0xFF 0xFF 0xFF) (0x0F 0xD7 0xFF) (0x69 0xA2 0xFF) (0xD4 0x80 0xFF)
         (0xFF 0x45 0xF3) (0xFF 0x61 0x8B) (0xFF 0x88 0x33) (0xFF 0x9C 0x12)
         (0xFA 0xBC 0x20) (0x9F 0xE3 0x0E) (0x2B 0xF0 0x35) (0x0C 0xF0 0xA4)
         (0x05 0xFB 0xFF) (0x5E 0x5E 0x5E) (0x0D 0x0D 0x0D) (0x0D 0x0D 0x0D)
         (0xFF 0xFF 0xFF) (0xA6 0xFC 0xFF) (0x83 0xEC 0xFF) (0xDA 0xAB 0xEB)
         (0xFF 0xA8 0xF9) (0xFF 0xAB 0x83) (0xFF 0xD2 0xB0) (0xFF 0xEF 0xA6)
         (0xFF 0xF7 0x9C) (0xD7 0xE8 0x95) (0xA6 0xED 0xAF) (0xA2 0xF2 0xDA)
         (0x99 0xFF 0xFC) (0xDD 0xDD 0xDD) (0x11 0x11 0x11) (0x11 0x11 0x11)]))

(def background-palette
  {:grey [0x0E 0x00 0x10 0x20] 
   :blue [0x0E 0x11 0x2C 0x31] 
   :red [0x0E 0x06 0x16 0x26] 
   :green [0x0E 0x1A 0x2A 0x20]})

(def sprite-palette
  {:grey [0x00 0x3D 0x2D 0x00] 
   :blue [0x00 0x11 0x2C 0x20] 
   :red [0x00 0x06 0x16 0x38] 
   :green [0x00 0x09 0x19 0x3B]})

(def block-defs
  `[{:name floor :tile-offset 4 :palette :grey}
    {:name wall :tile-offset 0 :palette :grey}
    {:name chip :tile-offset 8 :palette :grey}
    {:name water :tile-offset 12 :palette :grey}
    {:name fire :tile-offset 72 :palette :red}
    {:name invisible-wall :tile-offset 4 :palette :grey}
    {:name blocked-north :tile-offset 80 :palette :grey}
    {:name blocked-west :tile-offset 84 :palette :grey}
    {:name blocked-south :tile-offset 92 :palette :grey}
    {:name blocked-east :tile-offset 76 :palette :grey}
    {:name movable-dirt-block :tile-offset 0 :palette :red}
    {:name dirt-in-water :tile-offset 68 :palette :red}
    {:name ice :tile-offset 20 :palette :blue}
    {:name force-south :tile-offset 32 :palette :green}
    {:name cloning-block-north :tile-offset 0 :palette :red}
    {:name cloning-block-west :tile-offset 0 :palette :red}
    {:name cloning-block-south :tile-offset 0 :palette :red}
    {:name cloning-block-east :tile-offset 0 :palette :red}
    {:name force-north :tile-offset 16 :palette :green}
    {:name force-east :tile-offset 36 :palette :green}
    {:name force-west :tile-offset 40 :palette :green}
    {:name exit :tile-offset 64 :palette :blue}
    {:name blue-door :tile-offset 24 :palette :blue}
    {:name red-door :tile-offset 24 :palette :red}
    {:name green-door :tile-offset 24 :palette :green}
    {:name yellow-door :tile-offset 24 :palette :grey}
    {:name ice-south-east :tile-offset 44 :palette :blue}
    {:name ice-south-west :tile-offset 52 :palette :blue}
    {:name ice-north-west :tile-offset 56 :palette :blue}
    {:name ice-north-east :tile-offset 48 :palette :blue}
    {:name blue-block-tile :tile-offset 0 :palette :blue}
    {:name blue-block-wall :tile-offset 0 :palette :blue}
    {:name unused-bg :tile-offset 4 :palette :grey}
    {:name thief :tile-offset 156 :palette :blue}
    {:name chip-socket :tile-offset 28 :palette :grey}
    {:name green-button :tile-offset 104 :palette :green}
    {:name red-button :tile-offset 104 :palette :red}
    {:name switch-block-closed :tile-offset 132 :palette :green}
    {:name switch-block-open :tile-offset 128 :palette :green}
    {:name brown-button :tile-offset 104 :palette :grey}
    {:name blue-button :tile-offset 104 :palette :blue}
    {:name teleport :tile-offset 100 :palette :blue}
    {:name bomb :tile-offset 144 :palette :red}
    {:name trap :tile-offset 140 :palette :grey}
    {:name invisible-wall :tile-offset 4 :palette :grey}
    {:name gravel :tile-offset 96 :palette :red}
    {:name pass-once :tile-offset 0 :palette :grey}
    {:name hint :tile-offset 60 :palette :grey}
    {:name blocked-south :tile-offset 0 :palette :grey}
    {:name cloning-machine :tile-offset 108 :palette :grey}
    {:name force-all-directions :tile-offset 0 :palette :green}])

(def object-defs
 `({:name chip
    :id 1
    :tiles {:right (33 34 35 36)
            :left (37 38 39 40)
            :down (9 10 11 12)
            :up (41 42 43 44)}
    :palette :blue}
   {:name grey-key
    :id 2
    :tiles {:right-tiles (1 2 3 4)
            :left (1 2 3 4)
            :up (1 2 3 4)
            :down (1 2 3 4)}
    :palette :grey}
   {:name blue-key
    :id 3
    :tiles {:right (1 2 3 4)
            :left (1 2 3 4)
            :up (1 2 3 4)
            :down (1 2 3 4)}
    :palette :blue}
   {:name red-key
    :id 4
    :tiles {:right (1 2 3 4)
            :left (1 2 3 4)
            :up (1 2 3 4)
            :down (1 2 3 4)}
    :palette :red}
   {:name green-key
    :id 5
    :tiles {:right (1 2 3 4)
            :left (1 2 3 4)
            :up (1 2 3 4)
            :down (1 2 3 4)}
    :palette :green}
   {:name fire-shoe
    :id 6
    :tiles {:right (13 14 15 16)
            :left (13 14 15 16)
            :up (13 14 15 16)
            :down (13 14 15 16)}
    :palette :red}
   {:name ice-skate
    :id 7
    :tiles {:right (13 14 17 18)
            :left (13 14 17 18)
            :up (13 14 17 18)
            :down (13 14 17 18)}
    :palette :blue}
   {:name suction-shoe
    :id 8
    :tiles {:right (13 14 19 20)
            :left (13 14 19 20)
            :up (13 14 19 20)
            :down (13 14 19 20)}
    :palette :green}
   {:name flipper
    :id 9
    :tiles {:right (21 22 23 24)
            :left (21 22 23 24)
            :up (21 22 23 24)
            :down (21 22 23 24)}
    :palette :blue}
   {:name dirt-block
    :id 10
    :tiles {:right (5 6 7 8)
            :left (5 6 7 8)
            :up (5 6 7 8)
            :down (5 6 7 8)}
    :palette :red}
   {:name monster
    :id 11
    :tiles {:right (121 122 123 124)
            :left (113 114 115 116)
            :down (117 118 119 120)
            :up (109 110 111 112)}
    :palette :green}
   {:name ball
    :id 12
    :tiles {:right (29 30 31 32)
            :left (29 30 31 32)
            :down (29 30 31 32)
            :up (29 30 31 32)}
    :palette :red}
   {:name tank
    :id 13
    :tiles {:right (89 90 91 92)
            :left (93 94 95 96)
            :down (85 86 87 88)
            :up (81 82 83 84)}
    :palette :blue}
   {:name bee
    :id 14
    :tiles {:right (65 66 67 68)
            :left (69 70 71 72)
            :down (77 78 79 80)
            :up (73 74 75 76)}
    :palette :red}
   {:name bug
    :id 15
    :tiles {:right (45 46 47 48)
            :left (45 46 47 48)
            :down (25 26 27 28)
            :up (25 26 27 28)}
    :palette :green}
   {:name glider
    :id 16
    :tiles {:right (49 50 51 52)
            :left (53 54 55 56)
            :down (57 58 59 60)
            :up (61 62 63 64)}
    :palette :blue}
   {:name fireball
    :id 17
    :tiles {:right (125 126 127 128)
            :left (125 126 127 128)
            :down (125 126 127 128)
            :up (125 126 127 128)}
    :palette :red}
   {:name kreppar
    :id 18
    :tiles {:right (129 130 131 132)
            :left (129 130 131 132)
            :up (129 130 131 132)
            :down (129 130 131 132)}
    :palette :green}))

(def chr-file "/Users/jaffe1/chip_ca65/src/chip.chr")
(def spr-file "/Users/jaffe1/chip_ca65/src/chip.spr")

(def chr-tileset (read-tiles chr-file))
(def spr-tileset (read-tiles spr-file))

;; Graphics loading

(defn- read-bytes
  "Read the bytes in the file given into a byte array"
  [filename]
  (with-open [in-stream (io/input-stream filename)]
    (doall ; this is to force evaluation when the file is open 
     (for [i (range 256)
           :let [bytes (byte-array 16)]]
       (do
         (.read in-stream bytes)
         bytes)))))

;; Tile functions

(defn- bytes->tile
  "Convert a NES format byte to a list of pixel colors (palette indices)"
  [bytes]
  (vec
   (let [[plane1 plane2] (split-in-half (map #(bit-array % 8) bytes))]
     (map (fn [row1 row2]
            (vec (map (fn [bit1 bit2]
                    (+ (* bit2 2) bit1))
                  row1 row2)))
          plane1 plane2))))

(defn- read-tiles
  "Read all 256 tiles from CHR file"
  [chr-file]
  (when (= (.length (io/as-file chr-file)) 4096)
    (let [byte-arrays (read-bytes chr-file)]
      (vec (map bytes->tile byte-arrays)))))

(defn- compose-metatile
  "Put together a metatile based on a list of tiles"
  [tileset metatile-def]
  (apply concat
         (map #(map concat (first %) (second %))
              (split-in-half (map #(nth tileset %)
                                  metatile-def)))))

(defn- lookup-color
  "Take in a palette (sprite or background), palette number and palette index and find the resulting color"
  [palette palette-key palette-index]
  (let [c (nth palette-rgb-map
               (nth (palette palette-key)
                    palette-index))]
    (if (and (= palette sprite-palette) (= palette-index 0))
      (color 0 0 0 0)
      c)))
      
(defn- get-metatile-pixels
  "Get a nested vector of the pixels of a metatile"
  [tileset tile-list palette palette-key]
  (let [metatile (compose-metatile tileset tile-list)]
    (vec (for [row metatile]
           (mapv #(lookup-color palette palette-key %) row)))))

(defn get-block-pixels
  "Get a nested vector of the pixels of a block"
  [{offset :tile-offset palette-key :palette}]
  (get-metatile-pixels chr-tileset (range offset (+ offset 4)) background-palette palette-key))
  
(defn get-object-pixels
  "Get a nested vector of the pixels of an object"
  [{palette-key :palette :as object} direction]
  (get-metatile-pixels spr-tileset ((object :tiles) direction) sprite-palette palette-key))

(defn- metatile->image [pixels]
  (let [image (new BufferedImage 16 16 (. BufferedImage TYPE_INT_RGB))
        g (.getGraphics image)]
    (dotimes [x 16]
      (dotimes [y 16]
        (doto g
          (.setColor (get-in pixels [y x]))
          (.fillRect x y 1 1))))
    image))

(def blocks
  (map #(hash-map :name (% :name) :image (metatile->image (get-block-pixels %)))
       block-defs))

(def objects
  (map #(assoc (dissoc % :tiles) :images
               (into {} (for [[k v] (% :tiles)]
                          [k (metatile->image (get-object-pixels % k))])))
       object-defs))

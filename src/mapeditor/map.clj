;; Chip's challenge Map editor
;; Map stuff

(ns mapeditor.map
  (:use [mapeditor.util]
        [clojure.java.io :as io]))

(def level-format
  '[(:number-of-chips byte)
    (:number-of-traps byte)
    (:number-of-teleports byte)
    (:level-data (rle 1024))
    (:number-of-objects byte)
    (:object-data (array :number-of-objects
                         [(:x byte)
                          (:y byte)
                          (:direction-and-id byte)]))
   (:trap-list (array :number-of-traps
                      [(:x1 byte)
                       (:y1 byte)
                       (:x2 byte)
                       (:y2 byte)]))
   (:teleport-list (array :number-of-teleports
                          [(:x byte)
                           (:y byte)]))])
   
(defn- rle-encode
  "RLE encode the given sequence to the form (n1 e1) (n2 e2) ..."
  [s]
  (mapv #(list (count %) (first %))
       (partition-by identity s)))

(defn- rle-encode-binary
  "Encode a sequence to a binary RLE sequence where 0xFF is the RLE identifier"
  [s]
  (->> (map #(if (number? %)
               %
               (let [[rep-count element] %]
                 (concat (list 0xFF (mod rep-count 256) element)
                         (repeat (int (/ rep-count 256))
                                 (list 0xFF 0 element)))))
            (rle-encode s))
       flatten
       (map uint->byte)
       byte-array))

(defn- rle-decode-binary
  "Decode given binary byte array until byte-count has been reached"
  [bytes byte-count]
  (loop [i byte-count
         bytes-left bytes
         decoded-bytes []]
    (if (zero? i)
      [decoded-bytes bytes-left]
      (let [head-byte (first bytes-left)]
        (if-not (= (first bytes-left) 0xFF)
          (recur (dec i) (rest bytes-left) (conj decoded-bytes head-byte))
          (let [value (second bytes-left)
                repetitions (if (zero? (nth bytes-left 2)) 0 256)]
            (recur (- i repetitions)
                   (drop 3 bytes-left)
                   (into decoded-bytes (repeat (if (zero? repetitions) 256 repetitions) value)))))))))

(defn- unpack-level
  "Unpack a given level byte-array by following the file format descriptor level-format."
  [binary-data]
  (letfn
     [(unpack-array [rep-count format binary-data]
        (loop [i rep-count
               bytes binary-data
               data-list ()]
          (if (zero? i)
            [data-list bytes]
            (let [[data bytes-left] (unpack bytes format)]
              (recur (dec i) bytes-left (conj data-list data))))))
      
      (unpack [binary-data format level-structure]
        (loop [level format
               level-structure {}
               bytes binary-data]
          (if (empty? level)
            [level-structure bytes]
            (let [current-element (first level)
                  [field-name field-type] current-element
                  [data bytes-left] (if (= field-type 'byte)
                                      [(first bytes) (rest bytes)]
                                      (condp = (first field-type)
                                        'array (let [size (get level-structure (second field-type))]
                                                 (if (zero? size)
                                                   [nil bytes]
                                                   (unpack-array (get level-structure (second field-type))
                                                                 (nth field-type 2)
                                                                 bytes)))
                                        'rle (rle-decode-binary bytes (second field-type))))]
              (recur (rest level)
                     (merge {field-name data} level-structure)
                     bytes-left)))))]
    (first (unpack binary-data level-format))))


(defn load-level
  "Reads a binary level file and decomposes it into a hash map representing the level."
  [filename]
  (with-open [in-stream (io/input-stream filename)]
    (let [file-size (.length (io/as-file filename))
          bytes (byte-array file-size)]
      (.read in-stream bytes)
      (unpack-level (map byte->int (into [] bytes))))))

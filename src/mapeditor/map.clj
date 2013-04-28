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
                          (:initial-direction byte)
                          (:id byte)]))
   (:trap-list (array :number-of-traps
                      [(:x1 byte)
                       (:y1 byte)
                       (:x2 byte)
                       (:y2 byte)]))
   (:teleport-list (array :number-of-teleports
                          [(:x byte)
                           (:y byte)]))])
   
(defn- rle-encode [s]
  "RLE encode the sequence s to the form [n1 e1] [n2 e2] ..."
  (letfn
      [(count-consecutive [element sq]
         (loop [rest-of-seq sq
                count 0]
           (if (= element (first rest-of-seq))
             (recur (rest rest-of-seq)
                    (inc count))
             count)))
       (group-consecutive [sq]
         (loop [rest-of-seq sq
                new-seq []]
           (if (empty? rest-of-seq)
             new-seq
             (let [cur-element (first rest-of-seq)
                   rep-count (count-consecutive cur-element rest-of-seq)]
               (recur (drop rep-count rest-of-seq)
                      (conj new-seq (if (= rep-count 1)
                                      cur-element
                                      [rep-count cur-element])))))))]
    (group-consecutive s)))

(defn- rle-encode-binary [s]
  (vec (flatten
        (map #(if (number? %) % (list 0xFF (first %) (second %)))
             (rle-encode s)))))

(defmacro take-when-zero [a b]
  `(if (zero? ~a)
     ~b ~a))

(defn- rle-decode-binary [bytes byte-count]
  "Decode given binary byte array until byte-count has been reached"
  (loop [i byte-count
         bytes-left bytes
         decoded-bytes []]
    (if (zero? i)
      [decoded-bytes bytes-left]
      (let [head-byte (first bytes-left)]
        (if-not (= (first bytes-left) 0xFF)
          (recur (dec i) (rest bytes-left) (conj decoded-bytes head-byte))
          (let [value (second bytes-left)
                repetitions (take-when-zero (nth bytes-left 2) 256)]
            (recur (- i repetitions)
                   (drop 3 bytes-left)
                   (into decoded-bytes (repeat (if (zero? repetitions) 256 repetitions) value)))))))))

(defn- unpack-level [binary-data]
  (letfn
     [(unpack-array [rep-count format binary-data]
        (loop [i rep-count
               bytes binary-data
               data-list ()]
          (if (zero? i)
            [data-list bytes]
            (let [[data bytes-left] (unpack bytes format)]
              (recur (dec i) bytes-left (conj data-list data))))))
      
      (unpack [binary-data format]
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

(defn load-level [filename]
  (with-open [in-stream (io/input-stream filename)]
    (let [file-size (.length (io/as-file filename))
          bytes (byte-array file-size)]
      (.read in-stream bytes)
      (unpack-level (map byte->int (into [] bytes))))))

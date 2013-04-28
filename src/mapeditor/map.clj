;; Chip's challenge Map editor
;; Map stuff

(ns mapeditor.map
  (:use [mapeditor.util]))

(def level-format
  '[(:number-of-chips byte)
    (:number-of-traps byte)
    (:number-of-teleports byte)
    (:level-data (rle 1024))
    (:number-of-objects byte)
    (:object-data (array :number-of-objects
                         [:x byte
                          :y byte
                          :initial-direction byte
                          :id byte]))
   (:trap-list (array :number-of-traps
                      [:x1 byte
                       :y1 byte
                       :x2 byte
                       :y2 byte]))
   (:teleport-list (array :number-of-teleports
                          [:x byte
                           :y byte]))])
   
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

(defn- rle-decode-binary [bytes byte-count]
  "Decode given binary byte array until byte-count has been reached"
  (loop [i byte-count
         bytes-left bytes
         decoded-bytes []]
    (if (or (zero? i) (empty? bytes-left))
      [decoded-bytes bytes-left]
      (let [head-byte (first bytes-left)]
        (if-not (= (first bytes-left) 0xFF)
          (recur (dec i) (rest bytes-left) (conj decoded-bytes head-byte))
          (let [repetitions (second bytes-left)
                value (nth bytes-left 2)]
            (recur (- i repetitions)
                   (drop 3 bytes-left)
                   (into decoded-bytes (repeat repetitions value)))))))))
   
(defn unpack-level [binary-data]
  (letfn
     [(unpack-array [[rep-count format] binary-data]
        (loop [i rep-count
               bytes binary-data
               data-list ()]
          (if (zero? i)
            [bytes data-list]
            (let [[data bytes-left] (unpack bytes format)]
              (recur (dec i) bytes (conj data-list data))))))
      
      (unpack [binary-data format]
        (loop [level format
               level-structure {}
               bytes binary-data]
          (let [current-element (first level)
                [field-name field-type] current-element
                [data bytes-left]
                (if (= field-type 'byte)
                  [(byte->int (first bytes)) (rest bytes)]
                  (condp = (first field-type)
                    'array (unpack-array (rest field-type) bytes)
                    'rle (rle-decode-binary bytes (second field-type))))]
            (recur (rest level)
                   (merge {field-name data})
                   bytes-left))))]
    (unpack binary-data level-format)))

;; Chip's challenge Map editor
;; Map stuff

(defn rle-encode [s]
  "RLE encode the sequence s to the form [n1 e1] [n2 e2] ..."
  (letfn
      [(count-consecutive [element sq]
         (loop [rest-of-seq sq count 0]
           (if (= element (first rest-of-seq))
             (recur (rest rest-of-seq) (inc count))
             count)))
       (group-consecutive [sq]
         (loop [rest-of-seq sq new-seq []]
           (if (empty? rest-of-seq)
             new-seq
             (let [cur-element (first rest-of-seq)
                   rep-count (count-consecutive cur-element rest-of-seq)]
               (recur (drop rep-count rest-of-seq)
                      (conj new-seq (if (= rep-count 1)
                                      cur-element
                                      [rep-count cur-element])))))))]
    (group-consecutive s)))

(defn rle-decode [s]
  "RLE decode the assumed encoded sequence s"
  (flatten
   (map #(if (vector? %)
           (repeat (first %) (second %))
           %)
        s)))


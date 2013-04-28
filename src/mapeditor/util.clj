;; A few utility functions for bit operations etc.

(ns mapeditor.util)

(defn split-in-half [seq]
  (let [n (count seq)
        s (if (even? n)
            (/ n 2) (/ (- n 1) 2))]
    (split-at s seq)))

(defn byte->int [byte]
  "Convert a byte to an unsigned integer"
  (let [val (int byte)]
    (if (>= val 0)
      val
      (+ val 256))))
     
(defn bit-count [x]
  "Count the number of bits in x"
  (inc (int (/ (Math/log x)
               (Math/log 2)))))

(defn get-bit [x n]
  "Get the nth bit in x"
  (if (bit-test x n) 1 0))

(defn bit-array [x & [n]]
  "Get an array of the used bits of x or the n first bits of x"
  (let [val (byte->int x)]
    (map #(get-bit val %) (vec (reverse (range (or n (bit-count val))))))))

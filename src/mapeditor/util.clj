;; A few utility functions for bit operations etc.

(ns mapeditor.util)

(defn split-in-half
  "Splits the given sequence in half"
  [seq]
  (let [n (count seq)
        s (if (even? n)
            (/ n 2) (/ (- n 1) 2))]
    (split-at s seq)))

(defn byte->int
  "Convert a byte to an unsigned integer"
  [byte]
  (let [val (int byte)]
    (if (>= val 0)
      val
      (+ val 256))))

(defn uint->byte
  "Convert integer between 0 and 255 to a signed byte"
  [val]
  (byte (if (<= val 127)
          val
          (- val 256))))

(defn bit-count
  "Count the number of bits in x"
  [x]
  (inc (int (/ (Math/log x)
               (Math/log 2)))))

(defn get-bit
  "Get the nth bit in x"
  [x n]
  (if (bit-test x n) 1 0))

(defn bit-array
  "Get an array of the used bits of x or the n first bits of x"
  [x & [n]]
  (let [val (byte->int x)]
    (map #(get-bit val %) (vec (reverse (range (or n (bit-count val))))))))

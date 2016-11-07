(ns simple-search.core
  (:use simple-search.knapsack-examples.knapPI_11_20_1000
        simple-search.knapsack-examples.knapPI_13_20_1000
        simple-search.knapsack-examples.knapPI_16_20_1000
        simple-search.knapsack-examples.knapPI_16_200_1000)
  (:require [clojure.math.numeric-tower :as math]))

;;; An answer will be a map with (at least) four entries:
;;;   * :instance
;;;   * :choices - a vector of 0's and 1's indicating whether
;;;        the corresponding item should be included
;;;   * :total-weight - the weight of the chosen items
;;;   * :total-value - the value of the chosen items

(defrecord Answer
  [instance choices total-weight total-value])

(defn included-items
  "Takes a sequences of items and a sequence of choices and
  returns the subsequence of items corresponding to the 1's
  in the choices sequence."
  [items choices]
  (map first
       (filter #(= 1 (second %))
               (map vector items choices))))

(defn make-answer
  [instance choices]
  (let [included (included-items (:items instance) choices)]
    (->Answer instance choices
              (reduce + (map :weight included))
              (reduce + (map :value included)))))

(defn random-answer
  "Construct a random answer for the given instance of the
  knapsack problem."
  [instance]
  (let [choices (repeatedly (count (:items instance))
                            #(rand-int 2))]
    (make-answer instance choices)))

; (random-answer knapPI_13_20_1000_7)

;;; It might be cool to write a function that
;;; generates weighted proportions of 0's and 1's.

(defn score
  "Takes the total-weight of the given answer unless it's over capacity,
   in which case we return 0."
  [answer]
  (if (> (:total-weight answer)
         (:capacity (:instance answer)))
    0
    (:total-value answer)))

(defn penalized-score
  "Takes the total-weight of the given answer unless it's over capacity,
   in which case we return the negative of the total weight."
  [answer]
  (if (> (:total-weight answer)
         (:capacity (:instance answer)))
    (- (:total-weight answer))
    (:total-value answer)))

(defn lexi-score
  [answer]
  (let [shuffled-items (shuffle (included-items (:items (:instance answer))
                                                (:choices answer)))
        capacity (:capacity (:instance answer))]
    (loop [value 0
           weight 0
           items shuffled-items]
      (if (empty? items)
        value
        (let [item (first items)
              w (:weight item)
              v (:value item)]
          (if (> (+ weight w) capacity)
            (recur value weight (rest items))
            (recur (+ value v)
                   (+ weight w)
                   (rest items))))))))

; (lexi-score (random-answer knapPI_16_200_1000_1))

(defn add-score
  "Computes the score of an answer and inserts a new :score field
   to the given answer, returning the augmented answer."
  [scorer answer]
  (assoc answer :score (scorer answer)))

(defn random-search
  [scorer instance max-tries]
  (apply max-key :score
         (map (partial add-score scorer)
              (repeatedly max-tries #(random-answer instance)))))

; (random-search penalized-score knapPI_16_200_1000_1 10000)

(defn mutate-choices
  [choices]
  (let [mutation-rate (/ 1 (count choices))]
    (map #(if (< (rand) mutation-rate) (- 1 %) %) choices)))

(defn mutate-answer
  [answer _ _]
  (make-answer (:instance answer)
               (mutate-choices (:choices answer))))

(defn mutate-choices-magic
  [choices k max-tries]
  (let [mutation-rate (+ 0.25 (* 0.50 (/ (- max-tries k) max-tries)))]
    (map #(if (< (rand) mutation-rate) (- 1 %) %) choices)))


(defn mutate-answer-magic
  [answer k max-tries]
  (make-answer (:instance answer)
               (mutate-choices-magic  (:choices answer) k max-tries)))

; (def ra (random-answer knapPI_11_20_1000_1))
; (mutate-answer ra)

(defn hill-climber
  [mutator scorer instance max-tries]
  (loop [current-best (add-score scorer (random-answer instance))
         num-tries 1]
    (let [new-answer (add-score scorer (mutator current-best num-tries max-tries))]
      (if (>= num-tries max-tries)
        current-best
        (if (> (:score new-answer)
               (:score current-best))
          (recur new-answer (inc num-tries))
          (recur current-best (inc num-tries)))))))


(defn wrong-simulated-annealing
  [mutator scorer instance max-tries]
  (loop [current (add-score scorer (random-answer instance))
         num-tries 1
         best current]
    (let [new-answer (add-score scorer (mutator current num-tries max-tries))
          temp (/ num-tries max-tries)]
      (if (>= num-tries max-tries)
        best
        (let [s (if (or (> (:score new-answer) (:score current))
                        (> (rand) temp))
                  new-answer
                  current)]
        (if (> (:score s) (:score best))
          (recur s (inc num-tries) s)
          (recur s (inc num-tries) best)))))))


(defn simulated-annealing
  [mutator scorer instance max-tries]
  (loop [current (add-score scorer (random-answer instance))
         num-tries 1
         best current]
    (let [new-answer (add-score scorer (mutator current num-tries max-tries))
          temp (/ num-tries max-tries)]
      (if (>= num-tries max-tries)
        best
        (let [s (if (or (> (:score new-answer) (:score current))
                        (< (rand) (math/expt 2.7182818284 (/ (- (:score new-answer) (:score current)) temp))))
                  new-answer
                  current)]
        (if (> (:score s) (:score best))
          (recur s (inc num-tries) s)
          (recur s (inc num-tries) best)))))))


; (time (random-search score knapPI_16_200_1000_1 100000
; ))

; (time (hill-climber mutate-answer score knapPI_16_200_1000_1 100000
; ))

; (time (hill-climber mutate-answer penalized-score knapPI_16_200_1000_1 100000
; ))

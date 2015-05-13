(ns musical-creativity.analyzer)

(defn group-by-time
  "takes a sequenrd progression."
  [events]
  (->> events
       (group-by :time)
       (sort-by first)
       (map last)))


(defn extract-voices-random
  "takes ments in the g
  n to one element."
  [grouped-events]
  (loop [acc      []
         material (map set grouped-events)]
    (if (every? #(= 1 %) (map count material))
      (conj acc (map first material))
      (let [picked (map (comp rand-nth vec) material)]
        (recur (conj acc picked)
               (map #(if (= 1 (count %1)) %1 (disj %1 %2))
                    material
                    picked))))))




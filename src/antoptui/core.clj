(ns antoptui.core
  (:use seesaw.core seesaw.graphics antopt.core)
  (:gen-class))

(defn paint-city
  [c g city]
   (let [[x y] city
    correctedx (* x 5)
    correctedy (* y 5)
    dot-style (style :background "#00bc00":stroke (stroke :width 3))]
   (do (draw g (circle correctedx correctedy 5) dot-style))))

(defn paint-cities
  [c g cities]
  (doseq [city cities] (paint-city c g city)))

(defn paint-connection
  [c g connection cities]
  (let [[city-id1 city-id2] connection
    correctedx1 (* (first (cities city-id1)) 5)
    correctedy1 (* (last (cities city-id1)) 5)
    correctedx2 (* (first (cities city-id2)) 5)
    correctedy2 (* (last (cities city-id2)) 5)
    line-style (style :foreground "#FF0000" :stroke 3 :cap :round)]
  (do (draw g (line correctedx1 correctedy1 correctedx2 correctedy2) line-style))))

(defn paint-tour
  [c g tour cities]
  (let [connections-in-tour (partition 2  1 (last tour))]
    (doseq [connection connections-in-tour] (paint-connection c g connection cities))
    (if (>= (count (last tour)) 2)
      (paint-connection c g [(first (last tour)) (peek (last tour))] cities))))

(defn paint
  [c g]
  (do (push g (paint-tour c g @shortest-tour cities-on-map)
      (paint-cities c g cities-on-map))))

(defn content-panel []
  (border-panel
   :center (canvas :id :antopt
                   :background "#ffffff"
                   :paint paint)))

(defn make-frame 
  [cities]
  (let [maxx (+ (* (reduce max (map first cities)) 5) 50)
        maxy (+ (* (reduce max (map last cities)) 5) 50)
        f (frame :title "Ant optimization"
                 :width maxx 
                 :height maxy
                 :on-close :dispose
                 :visible? true
                 :content (content-panel))]
    (do (.setLocation f (java.awt.Point. 100 300))
        (add-watch shortest-tour :wst (fn [key ref old new] (repaint! (select f [:#antopt])))))))
 

(defn -main [& args]
  "Main function to test the optimization"
  (native!)
  (make-frame cities-on-map)
  (let [shortest-antopt-tour (antopt cities-on-map)]
    (shutdown-agents)
    (println "Shortest Tour:" shortest-antopt-tour)))

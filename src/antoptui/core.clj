(ns antoptui.core
  (:use seesaw.core seesaw.graphics antopt.core)
  (:gen-class))

(defn paint
  []
  ())

(defn content-panel []
  (border-panel
   :center (canvas :id :antopt
                   :background "#000000"
                   :paint paint)))

(defn make-frame 
  [cities]
  (let [maxx (+ (* (reduce max (map first cities)) 5) 20)
        maxy (+ (* (reduce max (map last cities)) 5) 20)
        f (frame :title "Ant optimization"
                 :width maxx :height maxy
                 :on-close :dispose
                 :visible? true
                 :content (content-panel))]
    (.setLocation f (java.awt.Point. 100 300))))
 
(defn -main [& args]
  (native!)
  (make-frame antopt.core/cities-on-map))

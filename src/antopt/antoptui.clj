(ns antopt.antoptui
  (:use [antopt.core :exclude(-main)] seesaw.core seesaw.graphics)
  (:gen-class))

(def nodes (atom []))

(defn scaled-node-cordinates
  [[x y]]
  [(+ 10 (* x 5)) (+ 10 (* y 5))])

(defn paint-node
  [c g node]
  (let [[x y] (scaled-node-cordinates node)
        dot-style (style :background "#00bc00":stroke (stroke :width 3))]
    (draw g (circle x y 5) dot-style)))

(defn paint-nodes
  [c g nodes]
  (doseq [node nodes] (paint-node c g node)))

(defn paint-connection
  [c g [node-id1 node-id2] nodes]
  (let [[correctedx1 correctedy1] (scaled-node-cordinates (nodes node-id1))
        [correctedx2 correctedy2] (scaled-node-cordinates (nodes node-id2))
        line-style (style :foreground "#FF0000" :stroke 3 :cap :round)]
    (draw g (line correctedx1 correctedy1 correctedx2 correctedy2) line-style)))

(defn paint-tour
  [c g shortest-tour nodes]
  (let [connections-in-tour (partition 2  1 (shortest-tour :tour))]
    (doseq [connection connections-in-tour] (paint-connection c g connection nodes))))

(defn paint
  [c g]
  (paint-tour c g @shortest-tour @nodes)
  (paint-nodes c g @nodes))

(defn content-panel[]
  (border-panel
    :center (canvas :id :antopt
                    :background "#ffffff"
                    :paint paint)))

(defn make-frame 
  [nodes]
  (let [[scaled-max-x scaled-max-y] (scaled-node-cordinates [(reduce max (map first nodes)) (reduce max (map last nodes))])
        f (frame :title "Ant optimization"
                 :width (+ 50 scaled-max-x)
                 :height (+ 50 scaled-max-y)
                 :on-close :dispose
                 :visible? true
                 :content (content-panel))]
      (.setLocation f (java.awt.Point. 100 300))
      (add-watch shortest-tour :wst (fn [key ref old new] (repaint! (select f [:#antopt]))))))

(defn -main [& args]
  "Main function to test the optimization"
  ;(reset! nodes (read-edn-from-file-safely "tsmdata/belgiumtour.tsm"))
  (reset! nodes (read-edn-from-file-safely "tsmdata/xqf131.tsm"))
  ;(reset! nodes (read-edn-from-file-safely "tsmdata/eil51.tsm"))
  (native!)
  (make-frame @nodes)
  (let [shortest-antopt-tour (antopt @nodes)]
    (shutdown-agents)
    (println "Shortest Tour:" (shortest-antopt-tour :tour))
    (println "Length:" (shortest-antopt-tour :tour-length))))

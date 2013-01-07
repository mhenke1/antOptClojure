(ns antoptui.core
  (:use antopt.core seesaw.core seesaw.graphics)
  (:gen-class))

(def nodes (atom []))

(defn read-from-file-safely [filename]
  (with-open
      [r (java.io.PushbackReader.
        (clojure.java.io/reader filename))]
        (binding [*read-eval* false]
          (read r))))

(defn paint-node
  [c g node]
   (let [[x y] node
    correctedx (* x 5)
    correctedy (* y 5)
    dot-style (style :background "#00bc00":stroke (stroke :width 3))]
   (do (draw g (circle correctedx correctedy 5) dot-style))))

(defn paint-nodes
  [c g nodes]
  (doseq [node nodes] (paint-node c g node)))

(defn paint-connection
  [c g connection nodes]
  (let [[node-id1 node-id2] connection
    correctedx1 (* (first (nodes node-id1)) 5)
    correctedy1 (* (last (nodes node-id1)) 5)
    correctedx2 (* (first (nodes node-id2)) 5)
    correctedy2 (* (last (nodes node-id2)) 5)
    line-style (style :foreground "#FF0000" :stroke 3 :cap :round)]
  (do (draw g (line correctedx1 correctedy1 correctedx2 correctedy2) line-style))))

(defn paint-tour
  [c g tour nodes]
  (let [connections-in-tour (partition 2  1 (last tour))]
    (doseq [connection connections-in-tour] (paint-connection c g connection nodes))
    (if (>= (count (last tour)) 2)
      (paint-connection c g [(first (last tour)) (peek (last tour))] nodes))))

(defn paint
  [c g]
  (do (push g (paint-tour c g @shortest-tour @nodes))
      (paint-nodes c g @nodes)))

(defn content-panel[]
  (border-panel
   :center (canvas :id :antopt
                   :background "#ffffff"
                   :paint paint
                   )))

(defn make-frame 
  [nodes]
  (let [maxx (+ (* (reduce max (map first nodes)) 5) 50)
        maxy (+ (* (reduce max (map last nodes)) 5) 50)
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
  ;(reset! nodes (read-from-file-safely "tsmdata/xqf131.tsm"))
  (reset! nodes (read-from-file-safely "tsmdata/eil51.tsm"))
  (native!)
  (make-frame @nodes)
  (let [shortest-antopt-tour (antopt @nodes)]
    (shutdown-agents)
    (println "Shortest Tour:" shortest-antopt-tour)))

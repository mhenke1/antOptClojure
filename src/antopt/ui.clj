(ns antopt.ui
  "Graphical user interface for visualizing ant colony optimization.
   
   Displays nodes and the evolving best tour in real-time using Seesaw."
  (:require [antopt.core :as aco]
            [seesaw.core :as ss]
            [seesaw.graphics :as sg])
  (:gen-class))

;; UI State
(defonce ui-state
  (atom {:nodes []
         :best-tour {:length Long/MAX_VALUE
                     :path []}}))

;; Scaling and rendering
(def ^:private scale-factor 5)
(def ^:private padding 10)

(defn scale-coordinates
  "Scale node coordinates for display."
  [[x y]]
  [(+ padding (* x scale-factor))
   (+ padding (* y scale-factor))])

(defn paint-node
  "Draw a single node on the canvas."
  [graphics node]
  (let [[x y] (scale-coordinates node)
        style (sg/style :background "#00bc00"
                       :stroke (sg/stroke :width 3))]
    (sg/draw graphics (sg/circle x y 5) style)))

(defn paint-all-nodes
  "Draw all nodes on the canvas."
  [graphics nodes]
  (doseq [node nodes]
    (paint-node graphics node)))

(defn paint-connection
  "Draw a connection between two nodes."
  [graphics nodes [node-id1 node-id2]]
  (let [[x1 y1] (scale-coordinates (nodes node-id1))
        [x2 y2] (scale-coordinates (nodes node-id2))
        style (sg/style :foreground "#FF0000"
                       :stroke 3
                       :cap :round)]
    (sg/draw graphics (sg/line x1 y1 x2 y2) style)))

(defn paint-tour
  "Draw the complete tour path."
  [graphics nodes tour-path]
  (when (seq tour-path)
    (doseq [connection (partition 2 1 tour-path)]
      (paint-connection graphics nodes connection))))

(defn paint-canvas
  "Main paint function for the canvas."
  [canvas graphics]
  (let [{:keys [nodes best-tour]} @ui-state]
    (paint-tour graphics nodes (:path best-tour))
    (paint-all-nodes graphics nodes)))

;; UI Components
(defn create-canvas
  "Create the main drawing canvas."
  []
  (ss/canvas :id :antopt-canvas
             :background "#ffffff"
             :paint paint-canvas))

(defn create-frame
  "Create the main application frame."
  [nodes]
  (let [max-coords (map #(apply max %) (apply map vector nodes))
        [max-x max-y] (scale-coordinates max-coords)
        frame (ss/frame :title "Ant Colony Optimization - TSP"
                       :width (+ 50 max-x)
                       :height (+ 50 max-y)
                       :on-close :dispose
                       :content (create-canvas))]
    (.setLocation frame (java.awt.Point. 100 300))
    
    ;; Watch for tour updates and repaint
    (add-watch ui-state :tour-watcher
               (fn [_ _ _ new-state]
                 (ss/invoke-later
                   (ss/repaint! (ss/select frame [:#antopt-canvas])))))
    
    frame))

;; Optimization with UI updates
(defn optimize-with-ui
  "Run optimization while updating UI in real-time."
  [nodes config]
  (let [connections (aco/init-connections nodes config)
        distances (aco/extract-distances connections)
        initial-state (aco/create-state)]
    (loop [gen 0
           state initial-state
           conns connections]
      (when (zero? (mod gen 10))
        (println (format "Generation %d: Best length = %d"
                        gen
                        (:length (:shortest-tour state)))))
      
      ;; Update UI state
      (swap! ui-state assoc :best-tour (:shortest-tour state))
      
      (if (>= gen (:num-generations config))
        (:shortest-tour state)
        (let [{:keys [state connections]}
              (aco/process-generation state conns distances config)]
          (recur (inc gen) state connections))))))

;; Argument parsing (reuse from core)
(defn parse-int
  "Parse string to integer, return nil if invalid."
  [s]
  (try
    (Integer/parseInt s)
    (catch Exception _ nil)))

(defn parse-args
  "Parse command line arguments into a map.
   Supports both named parameters and positional filepath."
  [args]
  (loop [remaining args
         result {:filepath nil
                 :num-ants nil
                 :num-generations nil}]
    (if (empty? remaining)
      result
      (let [arg (first remaining)]
        (cond
          ;; Named parameters
          (or (= arg "-a") (= arg "--ants"))
          (recur (drop 2 remaining)
                 (assoc result :num-ants (parse-int (second remaining))))
          
          (or (= arg "-g") (= arg "--generations"))
          (recur (drop 2 remaining)
                 (assoc result :num-generations (parse-int (second remaining))))
          
          (or (= arg "-f") (= arg "--file"))
          (recur (drop 2 remaining)
                 (assoc result :filepath (second remaining)))
          
          ;; Filepath (first non-flag argument, for backward compatibility)
          (and (not (.startsWith arg "-"))
               (nil? (:filepath result)))
          (recur (rest remaining)
                 (assoc result :filepath arg))
          
          ;; Skip unknown flags
          :else
          (recur (rest remaining) result))))))

;; Main entry point
(defn -main
  "Launch the UI and run optimization.
   
   Usage:
     clojure -M:run [options]
   
   Options:
     -f, --file PATH        Path to TSM file (default: resources/xqf131.tsm)
     -a, --ants N           Number of ants per generation (default: 500)
     -g, --generations N    Number of generations to run (default: 125)
   
   Examples:
     clojure -M:run
     clojure -M:run -f resources/eil51.tsm
     clojure -M:run --file resources/eil51.tsm -a 300 -g 100
     clojure -M:run --ants 200 --generations 50
     clojure -M:run -f resources/eil51.tsm -a 300
     
   Note: Positional filepath (without -f) still supported for backward compatibility"
  [& args]
  (let [parsed (parse-args args)
        filepath (or (:filepath parsed) "resources/xqf131.tsm")
        num-ants (or (:num-ants parsed) (:num-ants aco/default-config))
        num-generations (or (:num-generations parsed) (:num-generations aco/default-config))
        nodes (aco/read-tsm-file filepath)
        config (assoc aco/default-config
                     :num-ants num-ants
                     :num-generations num-generations)]
    
    (println "=== Ant Colony Optimization - GUI ===")
    (println "Dataset:" filepath)
    (println "Number of cities:" (count nodes))
    (println "Ants per generation:" num-ants)
    (println "Number of generations:" num-generations)
    (println)
    
    ;; Initialize UI state
    (reset! ui-state {:nodes nodes
                      :best-tour {:length Long/MAX_VALUE
                                  :path []}})
    
    ;; Set native look and feel
    (ss/native!)
    
    ;; Create and show frame
    (-> (create-frame nodes)
        ss/show!)
    
    ;; Run optimization in background
    (future
      (let [result (optimize-with-ui nodes config)]
        (println "\n=== Optimization Complete ===")
        (println "Tour length:" (:length result))
        (println "Tour path:" (:path result))
        (shutdown-agents)))))

;; Made with Bob

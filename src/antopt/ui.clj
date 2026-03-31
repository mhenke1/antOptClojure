(ns antopt.ui
  "Graphical user interface for visualizing ant colony optimization.
   
   Displays nodes and the evolving best tour in real-time using Quil."
  (:require [antopt.core :as aco]
            [antopt.cli :as cli]
            [quil.core :as q]
            [quil.middleware :as m]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

;; UI State
(defonce ui-state
  (atom {:nodes []
         :best-tour {:length Long/MAX_VALUE
                     :path []}
         :generation 0
         :running true}))

;; Scaling and rendering
(def ^:private scale-factor 5)
(def ^:private padding 20)

(defn scale-coordinates
  "Scale node coordinates for display."
  [[x y]]
  [(+ padding (* x scale-factor))
   (+ padding (* y scale-factor))])

(defn draw-node
  "Draw a single node on the canvas."
  [node]
  (let [[x y] (scale-coordinates node)]
    (q/fill 0 188 0)
    (q/stroke 0 100 0)
    (q/stroke-weight 2)
    (q/ellipse x y 10 10)))

(defn draw-all-nodes
  "Draw all nodes on the canvas."
  [nodes]
  (doseq [node nodes]
    (draw-node node)))

(defn draw-connection
  "Draw a connection between two nodes."
  [nodes [node-id1 node-id2]]
  (let [[x1 y1] (scale-coordinates (nodes node-id1))
        [x2 y2] (scale-coordinates (nodes node-id2))]
    (q/stroke 255 0 0)
    (q/stroke-weight 2)
    (q/line x1 y1 x2 y2)))

(defn draw-tour
  "Draw the complete tour path."
  [nodes tour-path]
  (when (seq tour-path)
    (doseq [connection (partition 2 1 tour-path)]
      (draw-connection nodes connection))))

(defn setup
  "Setup function called once at the start."
  []
  (q/frame-rate 30)
  (q/smooth)
  @ui-state)

(defn update-state
  "Update function called each frame."
  [state]
  @ui-state)

(defn draw-state
  "Draw function called each frame."
  [state]
  ;; Clear background
  (q/background 255)
  
  ;; Draw tour
  (draw-tour (:nodes state) (:path (:best-tour state)))
  
  ;; Draw nodes
  (draw-all-nodes (:nodes state))
  
  ;; Draw info panel at bottom with semi-transparent background
  (let [panel-height 70
        panel-y (- (q/height) panel-height)]
    ;; Draw semi-transparent background for text
    (q/fill 255 255 255 230)
    (q/no-stroke)
    (q/rect 0 panel-y (q/width) panel-height)
    
    ;; Draw info text
    (q/fill 0)
    (q/text-size 14)
    (q/text (str "Generation: " (:generation state)) 10 (+ panel-y 20))
    (q/text (str "Best Tour Length: " (:length (:best-tour state))) 10 (+ panel-y 40))
    (q/text (str "Cities: " (count (:nodes state))) 10 (+ panel-y 60))))

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
      (swap! ui-state assoc 
             :best-tour (:shortest-tour state)
             :generation gen)
      
      ;; Small delay to allow UI to update
      (Thread/sleep 10)
      
      (if (>= gen (:num-generations config))
        (:shortest-tour state)
        (let [{:keys [state connections]}
              (aco/process-generation state conns distances config)]
          (recur (inc gen) state connections))))))

;; CLI options
(def cli-options
  "Command line options specification for tools.cli."
  (cli/make-cli-options "resources/xqf131.tsm"
                        (:num-ants aco/default-config)
                        (:num-generations aco/default-config)))

;; Main entry point
(defn -main
  "Launch the UI and run optimization.
   
   Usage:
     clojure -M:run [options]
   
   Options:
     -f, --file PATH        Path to TSM file (default: resources/xqf131.tsm)
     -a, --ants N           Number of ants per generation (default: 500)
     -g, --generations N    Number of generations to run (default: 125)
     -h, --help             Show help message
   
   Examples:
     clojure -M:run
     clojure -M:run -f resources/eil51.tsm
     clojure -M:run --file resources/eil51.tsm -a 300 -g 100
     clojure -M:run --ants 200 --generations 50
     clojure -M:run -f resources/eil51.tsm -a 300"
  [& args]
  (let [exit-code (cli/handle-cli
                    args
                    cli-options
                    "Ant Colony Optimization - GUI (Quil)"
                    (fn [options]
                      (let [filepath (:file options)
                            num-ants (:ants options)
                            num-generations (:generations options)
                            config (assoc aco/default-config
                                         :num-ants num-ants
                                         :num-generations num-generations)
                            nodes (aco/read-tsm-file filepath)
                            max-coords (map #(apply max %) (apply map vector nodes))
                            [max-x max-y] (scale-coordinates max-coords)
                            width (+ 100 max-x)
                            height (+ 100 max-y)]
                        
                        (println "=== Ant Colony Optimization - GUI (Quil) ===")
                        (println "Dataset:" filepath)
                        (println "Number of cities:" (count nodes))
                        (println "Ants per generation:" num-ants)
                        (println "Number of generations:" num-generations)
                        (println)
                        
                        ;; Initialize UI state
                        (reset! ui-state {:nodes nodes
                                          :best-tour {:length Long/MAX_VALUE
                                                      :path []}
                                          :generation 0
                                          :running true})
                        
                        ;; Run optimization in background
                        (future
                          (let [result (optimize-with-ui nodes config)]
                            (println "\n=== Optimization Complete ===")
                            (println "Tour length:" (:length result))
                            (println "Tour path:" (:path result))
                            (swap! ui-state assoc :running false)
                            (shutdown-agents)))
                        
                        ;; Start Quil sketch
                        (q/defsketch aco-visualization
                          :title "Ant Colony Optimization - TSP"
                          :size [width height]
                          :setup setup
                          :update update-state
                          :draw draw-state
                          :middleware [m/fun-mode]
                          :features [:keep-on-top]))))]
    (when (not= exit-code 0)
      (System/exit exit-code))))

;; Made with Bob

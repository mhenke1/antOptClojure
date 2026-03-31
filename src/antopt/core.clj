(ns antopt.core
  "Ant Colony Optimization for the Traveling Salesman Problem.
   
   This implementation uses pheromone trails and heuristic information
   to find near-optimal solutions to TSP instances."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [antopt.cli :as cli]))

;; Algorithm parameters
(def default-config
  "Default configuration parameters for the ACO algorithm."
  {:alpha 1.0              ; Pheromone importance
   :beta 2.0               ; Distance importance
   :rho 0.25               ; Evaporation rate
   :num-ants 500           ; Ants per generation
   :num-generations 125})  ; Number of iterations

;; Connection information record
(defrecord ConnectionInfo [distance weighted-distance tau probability])

;; State management
(defn create-state
  "Create initial algorithm state."
  []
  {:shortest-tour {:length Long/MAX_VALUE
                   :path []}
   :generation 0})

;; File I/O
(defn read-tsm-file
  "Read TSM (Traveling Salesman) data from an EDN file."
  [filepath]
  (with-open [reader (io/reader filepath)]
    (edn/read (java.io.PushbackReader. reader))))

;; Distance calculations
(defn euclidean-distance
  "Calculate Euclidean distance between two points."
  [[x1 y1] [x2 y2]]
  (Math/sqrt (+ (Math/pow (- x2 x1) 2)
                (Math/pow (- y2 y1) 2))))

(defn connection-distance
  "Calculate rounded Euclidean distance between two nodes.
   Returns 0 for same node, rounded distance otherwise."
  [node-id1 node-id2 nodes]
  (if (= node-id1 node-id2)
    0
    (-> (euclidean-distance (nodes node-id1) (nodes node-id2))
        Math/round)))

(defn tour-length
  "Calculate total length of a tour given distance lookup."
  [distance-map tour]
  (transduce (map distance-map)
             +
             (partition 2 1 tour)))

(def memoized-tour-length
  "Memoized version of tour-length for performance."
  (memoize tour-length))

;; Connection initialization
(defn init-connection
  "Initialize connection data between two nodes."
  [node-id1 node-id2 nodes {:keys [alpha beta]}]
  (let [distance (connection-distance node-id1 node-id2 nodes)
        weighted-distance (Math/pow distance beta)
        tau (rand 0.1)
        weighted-tau (Math/pow tau alpha)
        probability (/ weighted-tau weighted-distance)]
    {[node-id1 node-id2]
     (->ConnectionInfo distance weighted-distance tau probability)}))

(defn init-connections
  "Initialize all connections between nodes."
  [nodes config]
  (let [node-count (count nodes)]
    (into {}
          (for [i (range node-count)
                j (range node-count)
                :when (not= i j)]
            (init-connection i j nodes config)))))

(defn extract-distances
  "Extract distance map from connection data."
  [connections]
  (into {}
        (map (fn [[conn-id info]]
               [conn-id (:distance info)]))
        connections))

;; Pheromone management
(defn evaporate-connection
  "Apply pheromone evaporation to a single connection."
  [{:keys [distance weighted-distance tau]} rho alpha]
  (let [new-tau (* tau (- 1.0 rho))
        new-weighted-tau (Math/pow new-tau alpha)
        new-probability (/ new-weighted-tau weighted-distance)]
    (->ConnectionInfo distance weighted-distance new-tau new-probability)))

(defn evaporate-all
  "Apply pheromone evaporation to all connections."
  [connections {:keys [rho alpha]}]
  (reduce-kv
    (fn [acc conn-id info]
      (assoc acc conn-id (evaporate-connection info rho alpha)))
    {}
    connections))

(defn deposit-pheromone
  "Deposit pheromone on a single connection based on tour quality."
  [connections conn-id tour-length {:keys [alpha]}]
  (let [{:keys [distance weighted-distance tau]} (connections conn-id)
        new-tau (+ tau (/ 1.0 tour-length))
        weighted-tau (Math/pow new-tau alpha)
        new-probability (/ weighted-tau weighted-distance)]
    (assoc connections conn-id
           (->ConnectionInfo distance weighted-distance new-tau new-probability))))

(defn deposit-tour-pheromone
  "Deposit pheromone along an entire tour."
  [connections {:keys [length path]} config]
  (reduce
    (fn [conns conn-id]
      (deposit-pheromone conns conn-id length config))
    connections
    (partition 2 1 path)))

(defn deposit-generation-pheromone
  "Deposit pheromone for all tours in a generation."
  [connections tours config]
  (reduce
    (fn [conns tour]
      (deposit-tour-pheromone conns tour config))
    connections
    tours))

;; Tour construction
(defn select-next-node
  "Select next node probabilistically based on pheromone and distance."
  [connections current-node remaining-nodes]
  (let [candidates (mapv (fn [node] [current-node node]) remaining-nodes)
        probabilities (mapv #(:probability (connections %)) candidates)
        cumulative-probs (reductions + probabilities)
        threshold (rand (last cumulative-probs))]
    (nth remaining-nodes
         (count (take-while #(< % threshold) cumulative-probs)))))

(defn build-tour
  "Construct a complete tour for one ant."
  [connections num-nodes]
  (loop [tour [0]
         remaining (vec (range 1 num-nodes))]
    (if (empty? remaining)
      (conj tour 0)  ; Return to start
      (let [next-node (select-next-node connections (peek tour) remaining)]
        (recur (conj tour next-node)
               (filterv #(not= % next-node) remaining))))))

(defn walk-ant
  "Create a tour for one ant and calculate its length."
  [distances connections num-nodes]
  (let [path (build-tour connections num-nodes)
        length (memoized-tour-length distances path)]
    {:length length :path path}))

;; Generation processing
(defn process-generation
  "Process one generation of ants."
  [state connections distances config]
  (let [{:keys [num-ants]} config
        num-nodes (count (set (mapcat identity (keys connections))))
        tours (pmap (fn [_] (walk-ant distances connections num-nodes))
                    (range num-ants))
        best-tour (apply min-key :length tours)
        new-connections (-> connections
                           (deposit-generation-pheromone tours config)
                           (evaporate-all config))
        new-state (-> state
                     (update :generation inc)
                     (update :shortest-tour
                             (fn [current]
                               (if (< (:length best-tour) (:length current))
                                 best-tour
                                 current))))]
    {:state new-state
     :connections new-connections}))

;; Main algorithm
(defn optimize
  "Run ant colony optimization algorithm.
   
   Parameters:
   - nodes: Vector of [x y] coordinates
   - config: Optional configuration map (uses defaults if not provided)
   
   Returns: Map with :length and :path of best tour found"
  ([nodes] (optimize nodes default-config))
  ([nodes config]
   (let [config (merge default-config config)
         connections (init-connections nodes config)
         distances (extract-distances connections)
         initial-state (create-state)]
     (loop [gen 0
            state initial-state
            conns connections]
       (when (zero? (mod gen 10))
         (println (format "Generation %d: Best length = %d"
                         gen
                         (:length (:shortest-tour state)))))
       (if (>= gen (:num-generations config))
         (:shortest-tour state)
         (let [{:keys [state connections]}
               (process-generation state conns distances config)]
           (recur (inc gen) state connections)))))))

;; CLI entry point
(def cli-options
  "Command line options specification for tools.cli."
  (cli/make-cli-options "resources/bier127.tsm"
                        (:num-ants default-config)
                        (:num-generations default-config)))

(defn -main
  "Run optimization from command line.
   
   Usage:
     clojure -M -m antopt.core [options]
   
   Options:
     -f, --file PATH        Path to TSM file (default: resources/bier127.tsm)
     -a, --ants N           Number of ants per generation (default: 500)
     -g, --generations N    Number of generations to run (default: 125)
     -h, --help             Show help message
   
   Examples:
     clojure -M -m antopt.core
     clojure -M -m antopt.core -f resources/eil51.tsm
     clojure -M -m antopt.core --file resources/eil51.tsm -a 300 -g 100
     clojure -M -m antopt.core --ants 200 --generations 50
     clojure -M -m antopt.core -f resources/eil51.tsm -a 300"
  [& args]
  (let [exit-code (cli/handle-cli
                    args
                    cli-options
                    "Ant Colony Optimization for TSP"
                    (fn [options]
                      (let [filepath (:file options)
                            num-ants (:ants options)
                            num-generations (:generations options)
                            config (assoc default-config
                                         :num-ants num-ants
                                         :num-generations num-generations)
                            nodes (read-tsm-file filepath)]
                        (println "=== Ant Colony Optimization ===")
                        (println "Dataset:" filepath)
                        (println "Number of cities:" (count nodes))
                        (println "Ants per generation:" num-ants)
                        (println "Number of generations:" num-generations)
                        (println)
                        (let [result (optimize nodes config)]
                          (println "\n=== Optimization Complete ===")
                          (println "Tour length:" (:length result))
                          (println "Tour path:" (:path result))
                          (shutdown-agents)))))]
    (when (not= exit-code 0)
      (System/exit exit-code))))

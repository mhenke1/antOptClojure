(ns antopt.core
  "Ant Colony Optimization for the Traveling Salesman Problem.
   
   This implementation uses pheromone trails and heuristic information
   to find near-optimal solutions to TSP instances."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

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

(defn -main
  "Run optimization from command line.
   
   Usage:
     clojure -M -m antopt.core [options]
   
   Options:
     -f, --file PATH        Path to TSM file (default: resources/bier127.tsm)
     -a, --ants N           Number of ants per generation (default: 500)
     -g, --generations N    Number of generations to run (default: 125)
   
   Examples:
     clojure -M -m antopt.core
     clojure -M -m antopt.core -f resources/eil51.tsm
     clojure -M -m antopt.core --file resources/eil51.tsm -a 300 -g 100
     clojure -M -m antopt.core --ants 200 --generations 50
     clojure -M -m antopt.core -f resources/eil51.tsm -a 300
     
   Note: Positional filepath (without -f) still supported for backward compatibility"
  [& args]
  (let [parsed (parse-args args)
        filepath (or (:filepath parsed) "resources/bier127.tsm")
        num-ants (or (:num-ants parsed) (:num-ants default-config))
        num-generations (or (:num-generations parsed) (:num-generations default-config))
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
      (shutdown-agents))))

;; Made with Bob

(ns antopt.core
  (:require clojure.edn)
  (:gen-class))

(def alpha 1)
(def beta 2)
(def rho 0.25)
(def number-of-ants 500)
(def number-of-generations 125)

(def shortest-tour (atom {:tour-length Long/MAX_VALUE :tour []}))

(defrecord ConnectionInfo [distance weighted-distance tau probability])

(defn read-edn-from-file-safely 
  "Read edn data from a file savely"
  [filename]  
  (with-open
    [r (java.io.PushbackReader.
         (clojure.java.io/reader filename))]
    (clojure.edn/read r)))

(defn euclidean-distance 
  "Calculates euclidean distance between two given points"
  [[x1 y1] [x2 y2]] 
  (Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2))))

(defn length-of-connection 
  "Calculates euclidean distance between two given nodes and rounds it to the nearest integer to match tsplib results"
  [[node-id1 node-id2] node-data] 
  (if (= node-id1 node-id2) 0
    (Math/round (euclidean-distance (node-data node-id1) (node-data node-id2)))))

(defn length-of-tour-internal
  "Calculates the total length of a given tour"
  [distance-data tour] 
  (apply + (map distance-data (partition 2  1 tour))))

(def length-of-tour
   (memoize length-of-tour-internal))
  
(defn create-connection-data 
  "Inititialize all data for a connection between two nodes"
  [connection node-data]
  (let [distance (length-of-connection connection node-data)
        weighted-distance (Math/pow distance beta)
        tau (rand 0.1)
        weighted-tau (Math/pow tau alpha)
        probability (/ weighted-tau weighted-distance)]
    {connection (->ConnectionInfo distance weighted-distance tau probability)}))

(defn extract-distance-data 
  "Inititialize all data for a connection between two nodes"
  [connection-data]
  (into {} (for [[connection-id connection-info] connection-data] [connection-id (:distance connection-info)])))


(defn initialize-connections 
  "Inititialize the data of all connections between the given nodes"
  [node-data]
  (into {} (for [x (range (count node-data)) y (range (count node-data)) :when (not= x y)] (create-connection-data [x y] node-data))))

(defn evaporate-one-connection 
  "Evaporates pheromone on a connection between two nodes"
  [{:keys [distance weighted-distance tau]}] 
  (let [new-tau (* tau (- 1 rho))
        new-weighted-tau (Math/pow new-tau alpha)
        new-probability (/ new-weighted-tau weighted-distance)]
    (->ConnectionInfo distance weighted-distance new-tau new-probability)))

(defn evaporate-all-connections
  "Evaporates pheromone on all connections between two nodes"
  [connection-data]
  (zipmap (map first connection-data) (map (comp evaporate-one-connection last) connection-data)))

(defn adjust-pheromone-for-one-connection
  "Amplifies pehoromone a connection walked by an ant"
  [tour-length connection-data connection-id]
  (let [{:keys [distance weighted-distance tau]} (connection-data connection-id)
        new-tau (+ tau (/ 1 tour-length))
        weighted-tau (Math/pow new-tau alpha)
        new-probability (/ weighted-tau weighted-distance)
        new-connection-data (assoc connection-data connection-id 
                              (->ConnectionInfo distance weighted-distance new-tau new-probability))]
    new-connection-data))

(defn adjust-pheromone-for-tour
  "Amplifies pehoromone a tour walked by an ant"
  [connection-data {:keys [tour-length tour]}]
  (reduce (partial adjust-pheromone-for-one-connection tour-length) connection-data (partition 2  1 tour)))

(defn adjust-pheromone-for-multiple-tours
  "Amplifies pehoromone a tour walked by a generation of ants"
  [connection-data tours-with-length]
  (reduce adjust-pheromone-for-tour connection-data tours-with-length))

(defn choose-next-node-on-tour 
  "Chooses the next node to walk based on the given pheromone data"
  [connection-data current-node remaining-nodes]
  (let [current-node-list (vec (repeat (count remaining-nodes) current-node))
        connections (vec (map vector current-node-list remaining-nodes))
        added-connection-probabilities (reductions + (map #(:probability (connection-data %)) connections))
        limit (rand (last added-connection-probabilities))]
    (nth remaining-nodes (count (filter #(< % limit) added-connection-probabilities)))))

(defn add-next-node-to-tour
  "Returns a tour with another node addes based on the given pheromone data and a list of the remaining nodes"
  [connection-data {:keys [tour remaining-nodes]}]
  (let [next-node (choose-next-node-on-tour connection-data (peek tour) remaining-nodes)]
    {:tour (conj tour next-node) :remaining-nodes (remove (partial = next-node) remaining-nodes)}))

(defn walk-ant-tour
  "Computes a tour passing all given nodes"
  [distance-data connection-data number-of-nodes]
  (let [tour ((nth (iterate (partial add-next-node-to-tour connection-data) {:tour [0] :remaining-nodes (range 1 number-of-nodes)}) (dec number-of-nodes)) :tour)]
    {:tour-length (length-of-tour distance-data (conj tour 0)) :tour (conj tour 0)}))

(defn one-generation-ant-tours
  "Computes tours passing all given nodes concurrently for a given number of ants"
  [number-of-ants number-of-nodes distance-data connection-data generation]
  (let [tour-list (pmap (fn [ant] (walk-ant-tour distance-data connection-data number-of-nodes)) (range number-of-ants))
        generation-shortest-tour (apply min-key :tour-length tour-list)
        new-connection-data (-> connection-data (adjust-pheromone-for-multiple-tours tour-list) evaporate-all-connections)]
    (print "Generation:" generation)
    (when (< (:tour-length generation-shortest-tour) (:tour-length @shortest-tour))
      (print " Length:" (:tour-length generation-shortest-tour))
      (reset! shortest-tour generation-shortest-tour))
    (println)       
    new-connection-data))

(defn antopt
  "Computes the shortest tour through a number of given nodes using ant colony optimization"
  [node-data]
  (let [connection-data (initialize-connections node-data)]
    (reduce (partial one-generation-ant-tours number-of-ants (count node-data) (extract-distance-data connection-data)) connection-data (range 1 (inc number-of-generations)))
    @shortest-tour))

(defn -main 
  "Main function to test the optimization"
  [& args]
  (let [node-data (read-edn-from-file-safely "tsmdata/bier127.tsm")
        shortest-antopt-tour (antopt node-data)]
    (shutdown-agents)
    (println "Shortest Tour:" (:tour shortest-antopt-tour))
    (println "Length:" (:tour-length shortest-antopt-tour))))

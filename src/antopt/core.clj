(ns antopt.core
  (:use [clojure.math.combinatorics :only (cartesian-product)])
  (:gen-class))
  
(def alpha 1)
(def beta 2)
(def rho 0.25)
(def number-of-ants 500)
(def number-of-generations 125)

(def shortest-tour (atom [Long/MAX_VALUE []]))

(defn euclidian-distance 
	"Calculates euclidian distance between two given points"
	[point1 point2] 
	(let [[x1 y1] point1 [x2 y2] point2] 
		(Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))

(defn length-of-connection 
	"Calculates euclidian distance between two given nodes"
	[connection nodes] 
	(let [[node-id1 node-id2] connection]
		(if (= node-id1 node-id2) 0.0
			(euclidian-distance (nodes node-id1) (nodes node-id2)))))
    
(defn length-of-tour
	"Calculates the total length of a given tour"
	[tour nodes] 
    (let [connections-in-tour (partition 2  1 tour)]
    	 (apply + (map #(length-of-connection  % nodes) connections-in-tour))))	

(defn create-connection-data 
	"Inititialize all data for a connection between two nodes"
	[connection nodes]
	(let [distance (length-of-connection connection nodes)
		  weighted-distance (Math/pow distance beta) 
		  tau (* (rand) 0.1)
		  weighted-tau (Math/pow tau alpha)
		  probability (/ weighted-tau weighted-distance)]
		  {:distance distance :weighted-distance weighted-distance :tau tau :weighted-tau weighted-tau :probability probability}))

(defn initialize-all-connections 
	"Inititialize the data of all connections between the given nodes"
	[nodes] 
	(let [all-connections (filter (fn [[x y]] (not= x y)) (cartesian-product (range (count nodes)) (range (count nodes))))]
		(apply merge (map (fn [connection] {(vec connection) (create-connection-data connection nodes)}) all-connections))))

(defn evaporate-one-connection 
	"Evaporates pheromone on a connection between two nodes"
	[connection-data] 
	(let [[connection-id one-connection-data] connection-data
		{:keys [distance weighted-distance tau]} one-connection-data
		new-tau (* tau (- 1 rho))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)]
		{connection-id {:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability}}))

(defn evaporate-all-connections
	"Evaporates pheromone on all connections between two nodes"
	[connection-data]
  	(apply merge (map #(evaporate-one-connection %) connection-data)))

(defn adjust-pheromone-for-one-connection
	"Amplifies pehoromone a connection walked by an ant"
	[length-of-tour connection-data connection-id]
	(let [{:keys [distance weighted-distance tau]} (connection-data connection-id)
		new-tau (+ tau (/ 1 length-of-tour))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)
		new-connection-data (assoc connection-data connection-id 
			{:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability})]
	new-connection-data))

(defn adjust-pheromone-for-tour
	"Amplifies pehoromone a tour walked by an ant"
	[connection-data tour-with-length]
	(let [[length-of-tour tour] tour-with-length
		connections-in-tour (partition 2  1 tour)
		adjusted-connection-data (reduce (partial adjust-pheromone-for-one-connection length-of-tour) connection-data connections-in-tour)]
	adjusted-connection-data)) 
			

(defn adjust-pheromone-for-multiple-tours
        "Amplifies pehoromone a tour walked by a generation of ants"
        [connection-data tours-with-length]
        (reduce adjust-pheromone-for-tour connection-data tours-with-length))

(defn choose-next-node-on-tour 
	"Chooses the next node to walk based on the given pheromone data"
	[connection-data current-node remaining-nodes]
	(let [current-node-list (vec (repeat (count remaining-nodes) current-node))
		connections (vec (map vector current-node-list remaining-nodes))
		added-probabilities (reduce + (map (fn [connection] (:probability (connection-data connection))) connections))
		limit (* (rand) added-probabilities)]
		(loop [probabilities 0 next-node current-node remaining-connections connections]
			(if (and (< probabilities limit) (not (empty? remaining-connections)))				
				(let [new-probabilities (+ probabilities (:probability (connection-data (first remaining-connections))))]
					(recur new-probabilities (last (first remaining-connections)) (rest remaining-connections)))
				next-node))))

(defn walk-ant-tour
	"Computes a tour passing all given nodes"
	[connection-data nodes]
	(let [nodes-list (range 1 (count nodes))]
		(loop [tour [0] remaining-nodes nodes-list]
			(if (empty? remaining-nodes)
				[(length-of-tour tour nodes) (conj tour (first tour))]
				(let [next-node (choose-next-node-on-tour connection-data (peek tour) remaining-nodes)
					new-tour (conj tour next-node)
					new-remaining-nodes (remove #(= % next-node) remaining-nodes)]
					(recur new-tour new-remaining-nodes))))))

(defn one-generation-ant-tours
	"Computes tours passing all given nodes concurrently for a given number of ants"
	[connection-data number-of-ants nodes]
	(let [tour-list (pmap (fn [ant] (walk-ant-tour connection-data nodes)) (range number-of-ants))
		shortest-tour (apply min-key first tour-list)
		new-connection-data (-> connection-data (adjust-pheromone-for-multiple-tours tour-list) (evaporate-all-connections))]
		{:new-connection-data new-connection-data :generation-shortest-tour shortest-tour}))

(defn antopt
	"Computes the shortest tour through a number of given nodes using ant colony optimization"
	[nodes]
	(let [connection-data (initialize-all-connections nodes)]
		(loop [number-of-generations number-of-generations connection-data connection-data]
			(if (> number-of-generations 0) 
				(let [{:keys [new-connection-data generation-shortest-tour]} (one-generation-ant-tours connection-data number-of-ants nodes)]
					(println number-of-generations) 
					(if (>= (first generation-shortest-tour) (first @shortest-tour))
						(recur (- number-of-generations 1) new-connection-data)
						(do 
							(reset! shortest-tour generation-shortest-tour)
							(println @shortest-tour)
							(recur (- number-of-generations 1) new-connection-data))))
			@shortest-tour))))

; (defn -main [& args]
; 	"Main function to test the optimization"
; 	(let [shortest-antopt-tour (antopt nodes)]
; 		(shutdown-agents)
; 		(println "Shortest Tour:" shortest-antopt-tour)))

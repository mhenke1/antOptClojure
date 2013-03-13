(ns antopt.core
  (:refer-clojure :exclude [read])
  (:use [clojure.tools.reader.edn :only (read)])
  (:gen-class))
  
(def alpha 1)
(def beta 2)
(def rho 0.25)
(def number-of-ants 500)
(def number-of-generations 125)

(def shortest-tour (atom {:tour-length Long/MAX_VALUE :tour []}))

(defn read-edn-from-file-safely [filename]
  (with-open
      [r (java.io.PushbackReader.
        (clojure.java.io/reader filename))]
          (read r)))

(defn euclidian-distance 
	"Calculates euclidian distance between two given points"
	[point1 point2] 
	(let [[x1 y1] point1 [x2 y2] point2] 
		(Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))

(defn length-of-connection 
	"Calculates euclidian distance between two given nodes and rounds it to the nearest integer to match tsplib results"
	[connection nodes] 
	(let [[node-id1 node-id2] connection]
		(if (= node-id1 node-id2) 0
			(Math/round (euclidian-distance (nodes node-id1) (nodes node-id2))))))
    
(defn length-of-tour
	"Calculates the total length of a given tour"
	[connection-data tour] 
    (let [connections-in-tour (partition 2  1 tour)]
    	  (apply + (map #(:distance (connection-data %)) connections-in-tour))))	

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
	(let [all-connections (for [x (range (count nodes)) y (range (count nodes)) :when (not= x y)] [x y])]
		(zipmap all-connections (map #(create-connection-data % nodes) all-connections))))

(defn evaporate-one-connection 
	"Evaporates pheromone on a connection between two nodes"
	[one-connection-data] 
	(let [{:keys [distance weighted-distance tau]} one-connection-data
		new-tau (* tau (- 1 rho))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)]
		{:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability}))

(defn evaporate-all-connections
	"Evaporates pheromone on all connections between two nodes"
	[connection-data]
	(zipmap (map #(first %) connection-data) (map #(evaporate-one-connection (last %)) connection-data)))

(defn adjust-pheromone-for-one-connection
	"Amplifies pehoromone a connection walked by an ant"
	[tour-length connection-data connection-id]
	(let [{:keys [distance weighted-distance tau]} (connection-data connection-id)
		new-tau (+ tau (/ 1 tour-length))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)
		new-connection-data (assoc connection-data connection-id 
			{:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability})]
		new-connection-data))

(defn adjust-pheromone-for-tour
	"Amplifies pehoromone a tour walked by an ant"
	[connection-data tour-with-length]
	(let [{:keys [tour-length tour]} tour-with-length]
		(reduce (partial adjust-pheromone-for-one-connection tour-length) connection-data (partition 2  1 tour))))
			
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
       	limit (* (rand) (last added-connection-probabilities))]
      	(nth remaining-nodes (count (filter #(< % limit) added-connection-probabilities)))))

(defn add-next-node-to-tour
	"Returns a tour with another node addes based on the given pheromone data and a list of the remaining nodes"
	[connection-data tour-with-remaining-nodes]
	(let [{:keys [tour remaining-nodes]} tour-with-remaining-nodes
		next-node (choose-next-node-on-tour connection-data (peek tour) remaining-nodes)]
		{:tour (conj tour next-node) :remaining-nodes (remove #(= % next-node) remaining-nodes)}))

(defn walk-ant-tour
	"Computes a tour passing all given nodes"
	[connection-data nodes]
	(let [tour-rem (last (take (count nodes) (iterate (partial add-next-node-to-tour connection-data) {:tour [0] :remaining-nodes (range 1 (count nodes))})))
		  tour (tour-rem :tour)]
		 {:tour-length (length-of-tour connection-data (conj tour 0)) :tour (conj tour 0)}))
		 
(defn one-generation-ant-tours
	"Computes tours passing all given nodes concurrently for a given number of ants"
	[number-of-ants nodes connection-data generation]
	(let [tour-list (pmap (fn [ant] (walk-ant-tour connection-data nodes)) (range number-of-ants))
		generation-shortest-tour (apply min-key #(% :tour-length) tour-list)
		new-connection-data (-> connection-data (adjust-pheromone-for-multiple-tours tour-list) (evaporate-all-connections))]
		(print "Generation:" generation)
		(when (< (generation-shortest-tour :tour-length) (@shortest-tour :tour-length))
				(print " Length:" (generation-shortest-tour :tour-length))
				(reset! shortest-tour generation-shortest-tour))
		(println)		
		new-connection-data))

(defn antopt
	"Computes the shortest tour through a number of given nodes using ant colony optimization"
	[nodes]
	(reduce (partial one-generation-ant-tours number-of-ants nodes) (initialize-all-connections nodes) (range 1 (+ 1 number-of-generations)))
	@shortest-tour)

(defn -main [& args]
	"Main function to test the optimization"
	(let [ nodes (read-edn-from-file-safely "tsmdata/bier127.tsm")
		   shortest-antopt-tour (antopt nodes)]
		(shutdown-agents)
		(println "Shortest Tour:" (shortest-antopt-tour :tour))
		(println "Length:" (shortest-antopt-tour :tour-length))))

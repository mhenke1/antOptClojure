(ns antopt.core
  (:use [clojure.math.combinatorics :only (cartesian-product)]))
  
(def alpha 1)
(def beta 2)
(def rho 0.5)
(def number-of-ants 500)
(def number-of-generations 50) 
  
(def cities-on-map [
	[37, 52], [49, 49], [52, 64], [20, 26], [40, 30], [21, 47],
	[17, 63], [31, 62], [52, 33], [51, 21], [42, 41], [31, 32],
	[ 5, 25], [12, 42], [36, 16], [52, 41], [27, 23], [17, 33],
	[13, 13], [57, 58], [62, 42], [42, 57], [16, 57], [ 8, 52],
	[ 7, 38], [27, 68], [30, 48], [43, 67], [58, 48], [58, 27],
	[37, 69], [38, 46], [46, 10], [61, 33], [62, 63], [63, 69],
	[32, 22], [45, 35], [59, 15], [ 5,  6], [10, 17], [21, 10],
	[ 5, 64], [30, 15], [39, 10], [32, 39], [25, 32], [25, 55], 
	[48, 28], [56, 37], [30, 40]
])

(defn euclidian-distance 
	"Calculates euclidian distance between two given points"
	[point1 point2] 
	(let [[x1 y1] point1 [x2 y2] point2] 
		(Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))

(defn connection-length 
	"Calculates euclidian distance between two cities"
	[connection cities] 
	(let [[city-id1 city-id2] connection]
		(euclidian-distance (cities city-id1) (cities city-id2))))
    
(defn tour-length 
	"Calculates the total length of a given tour"
	[tour cities] 
    (let [connections-in-tour (partition 2  1 tour)
    	 tour-length  (reduce + (map #(connection-length  % cities) connections-in-tour))]
    	 (if (>= (count tour) 2) 
    	   		(+ tour-length (connection-length  [(first tour) (peek tour)] cities))
    	    	tour-length)))	

(defn create-connection-data 
	"Inititialize all data for a connection between two cities"
	[connection cities]
	(let [[city1 city2] connection
		  distance (connection-length connection cities)
		  weighted-distance (Math/pow distance beta) 
		  tau (* (rand) 0.1)
		  weighted-tau (Math/pow tau alpha)
		  probability (/ weighted-tau weighted-distance)]
		  {:distance distance :weighted-distance weighted-distance :tau tau :weighted-tau weighted-tau :probability probability}))

(defn initialize-all-connections 
	"Inititialize the data of all connections between the given cities"
	[cities] 
	(let [all-connections (filter (fn [[x y]] (not= x y)) (cartesian-product (range (count cities)) (range (count cities))))]
		(reduce merge (map (fn [connection] {(vec connection) (create-connection-data connection cities)}) all-connections))))

(defn evaporate-connection-data 
	"Evaporates pheromone on a connection between two cities"
	[connection-id connection-data] 
	(let [{:keys [distance weighted-distance tau]} connection-data
		new-tau (* tau (- 1 rho))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)]
		{connection-id {:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability}}))

(defn evaporate-all-connections
	"Evaporates pheromone on all connections between two cities"
	[connection-data]
  	(reduce merge (map (fn [connection] (evaporate-connection-data (first connection) (last connection))) connection-data)))

(defn adjust-pheromone-for-tour
	"Amplifies pehoromone a tour walked by an ant"
	[connection-data tour-with-length]
	(let [[tour-length tour] tour-with-length
		connections-in-tour (vec (map vec (partition 2  1 tour)))]
		(loop [connection-data connection-data connections-in-tour connections-in-tour]
			(if (empty? connections-in-tour)
				connection-data
				(let [connection-id (first connections-in-tour)
					connection-info (connection-data connection-id)
				 	{:keys [distance weighted-distance tau]} connection-info
					new-tau (+ tau (/ 1 tour-length))
					new-weighted-tau (Math/pow new-tau alpha)
					new-probability (/ new-weighted-tau weighted-distance)
					new-connection-data (assoc connection-data connection-id {:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability})]
					(recur new-connection-data (rest connections-in-tour)))))))

(defn adjust-pheromone-for-multiple-tours
	"Amplifies pehoromone a tour walked by a generation of ants"
	[connection-data tours-with-length]
	(if (not (empty? tours-with-length))
		(let [new-connection-data (adjust-pheromone-for-tour connection-data (first tours-with-length))]
			(recur new-connection-data (vec (rest tours-with-length))))
			connection-data)) 

(defn choose-next-city 
	[connection-data current-city remaining-cities]
	(let [current-city-list (vec (repeat (count remaining-cities) current-city))
		connections (vec (map vector current-city-list remaining-cities))
		added-probabilities (reduce + (map (fn [connection] (:probability (connection-data connection))) connections))
		limit (* (rand) added-probabilities)]
		(loop [probabilities 0 next-city current-city remaining-connections connections]
			(if (and (< probabilities limit) (not (empty? remaining-connections)))				
				(let [new-probabilities (+ probabilities (:probability (connection-data (first remaining-connections))))]
					(recur new-probabilities (last (first remaining-connections)) (rest remaining-connections)))
				next-city))))

(defn walk-ant-tour
	[connection-data cities]
	(let [cities-list (range 1 (count cities))]
		(loop [tour [0] remaining-cities cities-list]
			(if (or (empty? tour) (empty? remaining-cities))
				[(tour-length tour cities) tour]
				(let [next-city (choose-next-city connection-data (peek tour) remaining-cities)
					new-tour (conj tour next-city)
					new-remaining-cities (remove #(= % next-city) remaining-cities)]
					(recur new-tour new-remaining-cities))))))

(defn one-generation-ant-tours
	[connection-data ant-number cities]
	(let [tour-list (map (fn [ant] (walk-ant-tour connection-data cities)) (range ant-number))
		shortest-tour (apply min-key  first tour-list)
		reversed-shortest-tour [(first shortest-tour) (vec (reverse (last shortest-tour)))]
		new-connection-data (-> connection-data (adjust-pheromone-for-tour shortest-tour) (adjust-pheromone-for-tour reversed-shortest-tour) (evaporate-all-connections))]
		{:new-connection-data new-connection-data :generation-shortest-tour shortest-tour}))

(defn antopt
	[]
	(let [connection-data (initialize-all-connections cities-on-map)]
		(loop [number-of-generations number-of-generations connection-data connection-data shortest-tour [Long/MAX_VALUE []]]
			(if (> number-of-generations 0) 
				(let [{:keys [new-connection-data generation-shortest-tour]} (one-generation-ant-tours connection-data number-of-ants cities-on-map)]
					(if (>= (first generation-shortest-tour) (first shortest-tour))
						(recur (- number-of-generations 1) new-connection-data shortest-tour)
						(do 
							(println number-of-generations ":" generation-shortest-tour)
							(recur (- number-of-generations 1) new-connection-data generation-shortest-tour))))))))

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
	
(defn leg-distance 
	"Calculates euclidian distance between two cities"
	[leg cities] 
	(let [[city1 city2] leg]
		(euclidian-distance (cities city1) (cities city2))))
    
(defn tour-length 
	"Calculates the total length of a given tour"
	[tour cities] 
    (let [legs-in-tour (partition 2  1 tour)
    	 tour-length  (reduce + (map #(leg-distance % cities) legs-in-tour))]
    	 (if (>= (count tour) 2) 
    	   		(+ tour-length (leg-distance [(first tour) (peek tour)] cities))
    	    	tour-length)))	

(defn initialize-leg-info 
	"Inititialize all data of a connection between two cities"
	[leg cities]
	(let [[city1 city2] leg
		  distance (leg-distance leg cities)
		  weighted-distance (Math/pow distance beta) 
		  tau (* (rand) 0.1)
		  weighted-tau (Math/pow tau alpha)
		  probability (/ weighted-tau weighted-distance)]
		  {:distance distance :weighted-distance weighted-distance :tau tau :weighted-tau weighted-tau :probability probability}))
    
(defn initialize-leg-data 
	"Inititialize the data of all connections between the given cities"
	[cities] 
	(let [all-legs (filter (fn [[x y]] (not= x y)) (cartesian-product (range (count cities)) (range (count cities))))]
		(reduce merge (map (fn [leg] {(vec leg) (initialize-leg-info leg cities)}) all-legs))))
    
(defn evaporate-leg 
	"Evaporates pheromone on a connection between two cities"
	[leg-id leg-info] 
	(let [{:keys [distance weighted-distance tau]} leg-info
		new-tau (* tau (- 1 rho))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)]
		{leg-id {:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability}}))

(defn evaporate-pheromone 
	"Evaporates pheromone on all connections between two cities"
	[leg-data]
  	(reduce merge (map (fn [leg] (evaporate-leg (first leg) (last leg))) leg-data)))


(defn adjust-pheromone-for-tour
	"Amplifies pehoromone a tour walked by an ant"
	[leg-data tour-with-length]
	(let [[tour-length tour] tour-with-length
		legs-in-tour (vec (map vec (partition 2  1 tour)))]
		(loop [leg-data leg-data legs-in-tour legs-in-tour]
			(if (empty? legs-in-tour)
				leg-data
				(let [leg-id (first legs-in-tour)
					leg-info (leg-data leg-id)
				 	{:keys [distance weighted-distance tau]} leg-info
					new-tau (+ tau (/ 1 tour-length))
					new-weighted-tau (Math/pow new-tau alpha)
					new-probability (/ new-weighted-tau weighted-distance)
					new-leg-data (assoc leg-data leg-id {:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability})]
					(recur new-leg-data (rest legs-in-tour)))))))

(defn adjust-pheromone-for-tours
	"Amplifies pehoromone a tour walked by a generation of ants"
	[leg-data tours-with-length]
	(if (not (empty? tours-with-length))
		(let [new-leg-data (adjust-pheromone-for-tour leg-data (first tours-with-length))]
			(recur new-leg-data (vec (rest tours-with-length))))
			leg-data)) 

(defn choose-next-city 
	[leg-data current-city remaining-cities]
	(let [current-city-list (vec (repeat (count remaining-cities) current-city))
		connections (vec (map vector current-city-list remaining-cities))
		added-probabilities (reduce + (map (fn [connection] (:probability (leg-data connection))) connections))
		limit (* (rand) added-probabilities)]
		(loop [probabilities 0 next-city current-city remaining-connections connections]
			(if (and (< probabilities limit) (not (empty? remaining-connections)))				
				(let [new-probabilities (+ probabilities (:probability (leg-data (first remaining-connections))))]
					(recur new-probabilities (last (first remaining-connections)) (rest remaining-connections)))
				next-city))))

(defn ant-walk-tour
	[leg-data cities]
	(let [cities-list (range 1 (count cities))]
		(loop [tour [0] remaining-cities cities-list]
			(if (or (empty? tour) (empty? remaining-cities))
				[(tour-length tour cities) tour]
				(let [next-city (choose-next-city leg-data (peek tour) remaining-cities)
					new-tour (conj tour next-city)
					new-remaining-cities (remove #(= % next-city) remaining-cities)]
					(recur new-tour new-remaining-cities))))))
	
(defn one-generation-ant-tours
	[leg-data ant-number cities]
	(let [tour-list (map (fn [ant] (ant-walk-tour leg-data cities)) (range ant-number))
		shortest-tour (apply min-key first tour-list)
		new-leg-data (-> leg-data (adjust-pheromone-for-tour (vec (last shortest-tour))) (adjust-pheromone-for-tour (vec (reverse (last shortest-tour))) (evaporate-pheromone)))]
		[new-leg-data tour-list shortest-tour]))

(defn one-generation-ant-tours2
	[leg-data ant-number cities]
	(let [tour-list (map (fn [ant] (ant-walk-tour leg-data cities)) (range ant-number))
		shortest-tour (apply min-key first tour-list)
		new-leg-data (last shortest-tour)]
		(println tour-list ":" shortest-tour ":" new-leg-data)))

; (defn multi-generation-ant-tour
; 	[generations ant-number cities]
	; (let [leg-data (initialize-leg-data cities)]
; 		(loop [leg-data leg-data generations generations shortest-tour [10000000 []]]
; 			(let [[new-leg-data  tour-list new-shortest-tour] (one-generation-ant-tours leg-data ant-number cities)]
; 				(println min-tour)
; 				(if (= generations 0) 
; 					new-leg-data
; 					(recur new-leg-data (- generations 1) new-shortest-tor))))))

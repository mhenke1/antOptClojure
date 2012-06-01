(ns antopt.core
  (:use [clojure.math.combinatorics  :only (cartesian-product)]))
  
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

(defn distance [point1 point2] 
	(let [[x1 y1] point1 [x2 y2] point2] 
	(Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))
	
(defn leg-distance [leg] 
	(let [[point1 point2] leg]
	(distance point1 point2))) 	
	
(defn tour-length [tour cities] 
    (let [cities-in-tour (map cities tour) 
    	 legs-in-tour (partition 2  1 cities-in-tour)
    	 length-of-legs (map leg-distance legs-in-tour)]
    (reduce + length-of-legs)))	
    
(defn create-leg-info [leg cities]
	(let [[city1 city2] leg
		  leg-dist (distance (cities city1) (cities city2))
		  weighted-distance (Math/pow leg-dist beta) 
		  tau (* (rand) 0.1)
		  weighted-tau (Math/pow tau alpha)
		  probability (/ weighted-tau weighted-distance)]
		  {:distance leg-dist :weighted-distance weighted-distance :tau tau :weighted-tau weighted-tau :probability probability}))
    
(defn initialize-leg-data [cities] 
	(let [all-legs (cartesian-product (range (count cities)) (range (count cities)))]
		 (reduce merge (map (fn [leg] {(vec leg) (create-leg-info leg cities)}) all-legs))))
    
(defn evaporate-leg [leg] 
	(let [[leg-id {leg-dist :distance weighted-distance :weighted-distance tau :tau}] leg
		new-tau (* tau (- 1 rho))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)]
	{leg-id {:distance leg-dist :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability}}))

(defn evaporate-pheromone [leg-data]
   (reduce merge (map evaporate-leg leg-data)))

(defn add-propabilites [connections] 1)

(defn choose-connection [leg-data limit added-probabilities remaining-connections]
	(let [new-added-probabilities (+ added-probabilities (:probability (leg-data (first remaining-connections))))]
		(if (< new-added-probabilities limit) 
			(choose-connection leg-data limit new-added-probabilities (rest remaining-connections)))
	(first remaining-connections )))

(defn choose-next-city [leg-data current-city remaining-cities]
  (let [current-city-list (repeat (count remaining-cities) current-city)
        connections (map list (vector current-city-list) remaining-cities)
        limit (* (rand) (add-propabilites connections))]
  (last (leg-data choose-connection limit 0 connections))))

(defn walk-ant-tour [leg-data tour remaining-cities]
	(let [next-city (choose-next-city leg-data (first tour) remaining-cities)
	new-tour (list next-city tour)
	new-remaining-cities (remove #(= % next-city) remaining-cities)]
	(if (not-empty new-remaining-cities)
		(walk-ant-tour leg-data new-tour new-remaining-cities)
		(tour))))
   
(defn generate-ant-tour[cities] 
	(let [leg-data (initialize-leg-data cities)]
	(walk-ant-tour leg-data [] cities))
)

(defn ant-tour []
	(let [ant-tour (generate-ant-tour cities-on-map)]
	(ant-tour)))
	

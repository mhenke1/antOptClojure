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

(defn euclidian-distance [point1 point2] 
	(let [[x1 y1] point1 [x2 y2] point2] 
	(Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))
	
(defn leg-distance [leg cities] 
	(let [[city1 city2] leg]
	(euclidian-distance (cities city1) (cities city2))))
	
(defn tour-length [tour cities] 
    (let [legs-in-tour (partition 2  1 tour)
    	 length-of-legs (map #(leg-distance % cities) legs-in-tour)]
    (reduce + length-of-legs)))	
    
(defn create-leg-info [leg cities]
	(let [[city1 city2] leg
		  distance (leg-distance leg cities)
		  weighted-distance (Math/pow distance beta) 
		  tau (* (rand) 0.1)
		  weighted-tau (Math/pow tau alpha)
		  probability (/ weighted-tau weighted-distance)]
		  {:distance distance :weighted-distance weighted-distance :tau tau :weighted-tau weighted-tau :probability probability}))
    
(defn initialize-leg-data [cities] 
	(let [all-legs (filter (fn [[x y]] (not= x y)) (cartesian-product (range (count cities)) (range (count cities))))]
		 (reduce merge (map (fn [leg] {(vec leg) (create-leg-info leg cities)}) all-legs))))
    
(defn evaporate-leg [leg-id leg-info] 
	(let [{:keys [distance weighted-distance tau]} leg-info
		new-tau (* tau (- 1 rho))
		new-weighted-tau (Math/pow new-tau alpha)
		new-probability (/ new-weighted-tau weighted-distance)]
	{leg-id {:distance distance :weighted-distance weighted-distance :tau new-tau :weighted-tau new-weighted-tau :probability new-probability}}))

(defn evaporate-pheromone [leg-data]
   (reduce merge (map (fn [leg] (evaporate-leg (first leg) (last leg))) leg-data)))

(defn choose-connection [leg-data limit added-probabilities remaining-connections]
	(let [new-added-probabilities (+ added-probabilities (:probability (leg-data (first remaining-connections))))
		connection (first remaining-connections)
		new-remaining-connections (rest remaining-connections)]
		(if (or (>= new-added-probabilities limit) (empty? new-remaining-connections))
			connection
			(recur leg-data limit new-added-probabilities new-remaining-connections))))

(defn choose-next-city [leg-data current-city remaining-cities]
	(let [current-city-list (vec (repeat (count remaining-cities) current-city))
		connections (vec (map vector current-city-list remaining-cities))
		added-probabilities (reduce + (map (fn [connection] (:probability (leg-data connection))) connections))
		limit (* (rand) added-probabilities)
		connection (choose-connection leg-data limit 0 connections)
		next-city (last connection)]
        next-city))

(defn walk-ant-tour [leg-data tour remaining-cities]
	(let [next-city (choose-next-city leg-data (first tour) remaining-cities)
	new-tour (list next-city tour)
	new-remaining-cities (remove #(= % next-city) remaining-cities)]
	(if (empty? new-remaining-cities) 
		(tour)
		(recur leg-data new-tour new-remaining-cities))))
   
(defn generate-ant-tour[cities] 
	(let [leg-data (initialize-leg-data cities)]
	(walk-ant-tour leg-data [] cities))
)

(defn ant-tour []
	(let [ant-tour (generate-ant-tour cities-on-map)]
	(ant-tour)))

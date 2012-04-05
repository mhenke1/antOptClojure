(ns antopt.core)
  (:require clojure.math.combinatorics)

(def cities [
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

(def alpha 0.5)
(def beta 2)

(defn initialize-data [cities] 
	(let [leg-data {} 
		  all-legs (cartesian-product (range (count cities)) (range (count cities)))]
		 (reduce merge (map (fn [leg] (assoc leg-data leg 1)) all-legs))))

(defn distance [point1 point2] 
	(let [[x1 y1] point1 [x2 y2] point2] 
	(Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))

(defn leg-distance [leg] 
	(let [[point1 point2] leg]
	(distance point1 point2))) 	

(defn calculate-tour-length [tour cities] 
    (let 
        [cities-in-tour (map cities tour) 
    	 legs-in-tour (partition 2  1 cities-in-tour)
    	 length-of-legs (map leg-distance legs-in-tour)]
    (reduce + length-of-legs)))
	
(defn generate-ant-tour[cities] 
	(let [leg-data (initialize-data cities)]
	(leg-data)
)

(defn ant-tour []
	(let [ant-tour (generate-ant-tour cities)]
	(ant-tour)))
	
    
 

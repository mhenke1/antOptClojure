(ns antopt.test.core
  (:use [antopt.core])
  (:use [clojure.test]))

(def test-data {[2 1] {:distance 5.0, :weighted-distance 25.0, :tau 0.019983426066513734, :weighted-tau 0.019983426066513734, :probability 7.993370426605494E-4}, 
	[1 2] {:distance 5.0, :weighted-distance 25.0, :tau 0.08945806419434195, :weighted-tau 0.08945806419434195, :probability 0.003578322567773678}, 
	[1 0] {:distance 5.0, :weighted-distance 25.0, :tau 0.09253302650638885, :weighted-tau 0.09253302650638885, :probability 0.003701321060255554}, 
	[0 1] {:distance 5.0, :weighted-distance 25.0, :tau 0.049126426838469066, :weighted-tau 0.049126426838469066, :probability 0.001965057073538763},
	[0 2] {:distance 5.0, :weighted-distance 25.0, :tau 0.055126426838469066, :weighted-tau 0.05526426838469066, :probability 0.002365057073538763}})
 
(deftest test-euclidian-distance
	(is (= 0.0 (euclidian-distance [0 0] [0 0])))
	(is (= 5.0 (euclidian-distance [0 0] [4 3]))))

(deftest test-leg-distance
	(is (= 0.0 (leg-distance [0 1] [[0 0] [0 0]])))
	(is (= 5.0 (leg-distance [0 1] [[0 0] [4 3]]))))

(deftest test-tour-length
	(is (= 0 (tour-length [0] [0 0])))
	(is (= 10.0 (tour-length [0 1] [[0 0] [4 3]])))
	(is (= 10.0 (tour-length [0 2 1] [[0 0] [0 0] [4 3]]))))

(deftest test-create-leg-info 
	(let [test-info (create-leg-info [0 1] [[0 0] [4 3]])]
		(is (= 5.0 (:distance test-info)))
		(is (= 25.0 (:weighted-distance test-info)))
		(is (> 0.1 (:tau test-info))))) 

(deftest test-initialize-leg-data 
	(let [test-info1 (test-data [0 1])
		test-info2 (test-data [1 0])
		test-info3 (test-data [0 0])
		test-info4 (test-data [1 1])]
		(is (= 5.0 (:distance test-info1)))
		(is (= 5.0 (:distance test-info2)))
		(is (= nil (:distance test-info3)))
		(is (= nil (:distance test-info4)))
		(is (= (:distance test-info1) (:distance test-info2)))))

(deftest test-evaporate-leg
	(let [test-leg (test-data [0 1])
		 test-evap-data (evaporate-leg [0 1] test-leg)
		 test-evap (test-evap-data [0 1])]
		(is (= (:distance test-evap) (:distance test-leg)))
		(is (< (:tau test-evap) (:tau test-leg)))
		(is (< (:weighted-tau test-evap) (:weighted-tau test-leg)))
		(is (= (:weighted-distance test-evap) (:weighted-distance test-leg)))
		(is (< (:probability test-evap) (:probability test-leg)))))

(deftest test-evaporate-pheromone
	(let [test-evap-data (evaporate-pheromone test-data)
		test-leg1 (test-data [0 1])
		test-evap1 (test-evap-data [0 1])
		test-leg2 (test-data [1 0])
		test-evap2 (test-evap-data [1 0])]
		(is (= (:distance test-evap1) (:distance test-leg1)))
		(is (< (:tau test-evap1) (:tau test-leg1)))
		(is (< (:weighted-tau test-evap1) (:weighted-tau test-leg1)))
		(is (= (:weighted-distance test-evap1) (:weighted-distance test-leg1)))
		(is (< (:probability test-evap1) (:probability test-leg1)))
		(is (= (:distance test-evap2) (:distance test-leg2)))
		(is (< (:tau test-evap2) (:tau test-leg2)))
		(is (< (:weighted-tau test-evap2) (:weighted-tau test-leg2)))
		(is (= (:weighted-distance test-evap2) (:weighted-distance test-leg2)))
		(is (< (:probability test-evap2) (:probability test-leg2)))))

(deftest test-adjust-pheromone-for-tour-legs
	(let [evap-data (adjust-pheromone-for-tour-legs test-data [[0 1] [1 2]] 10)
		test-leg1 (test-data [0 1])
		test-leg2 (test-data [1 2])
		test-evap1 (evap-data [0 1])
		test-evap2 (evap-data [1 2])]
		(is (= (:distance test-evap1) (:distance test-leg1)))
		(is (> (:tau test-evap1) (:tau test-leg1)))
		(is (> (:weighted-tau test-evap1) (:weighted-tau test-leg1)))
		(is (= (:weighted-distance test-evap1) (:weighted-distance test-leg1)))
		(is (> (:probability test-evap1) (:probability test-leg1)))
		(is (= (:distance test-evap2) (:distance test-leg2)))
		(is (> (:tau test-evap2) (:tau test-leg2)))
		(is (> (:weighted-tau test-evap2) (:weighted-tau test-leg2)))
		(is (= (:weighted-distance test-evap2) (:weighted-distance test-leg2)))
		(is (> (:probability test-evap2) (:probability test-leg2)))))

(deftest test-adjust-pheromone-for-tour
	(let [evap-data (adjust-pheromone-for-tour test-data [0 1 2] [[0 0] [0 0] [4 3]])
		test-leg1 (test-data [0 1])
		test-leg2 (test-data [1 2])
		test-evap1 (evap-data [0 1])
		test-evap2 (evap-data [1 2])]
		(is (= (:distance test-evap1) (:distance test-leg1)))
		(is (> (:tau test-evap1) (:tau test-leg1)))
		(is (> (:weighted-tau test-evap1) (:weighted-tau test-leg1)))
		(is (= (:weighted-distance test-evap1) (:weighted-distance test-leg1)))
		(is (> (:probability test-evap1) (:probability test-leg1)))
		(is (= (:distance test-evap2) (:distance test-leg2)))
		(is (> (:tau test-evap2) (:tau test-leg2)))
		(is (> (:weighted-tau test-evap2) (:weighted-tau test-leg2)))
		(is (= (:weighted-distance test-evap2) (:weighted-distance test-leg2)))
		(is (> (:probability test-evap2) (:probability test-leg2)))))


(deftest test-choose-connection
	(let [connection1 (choose-connection test-data 0.00197 0 [[0 1] [1 2] [1 0]])
		connection2 (choose-connection test-data 0.00194 0 [[0 1] [1 2] [1 0]])
		connection3 (choose-connection test-data 0.1 0 [[0 1] [1 2] [1 0]])
		connection4 (choose-connection test-data 0.0092 0 [[0 1] [1 2] [1 0]])
		connection5 (choose-connection test-data 0 0 [[0 1] [1 2] [1 0]])
		connection6 (choose-connection test-data 0 0 [])]
		(is (= connection1 [1 2]))
		(is (= connection2 [0 1]))
		(is (= connection3 [1 0]))
		(is (= connection4 [1 0]))
		(is (= connection5 [0 1]))
		(is (= connection6 []))))

(deftest test-choose-next-city
	(let [next-city1 (choose-next-city test-data 0 [1 2])
		next-city2 (choose-next-city test-data 0 [])]
		(is (some #{next-city1} [1 2]))
		(is (= next-city2 []))))

(deftest test-ant-walk-city-by-city 
	(let [ant-tour (ant-walk-city-by-city test-data [0] [1 2])]
		(is (some #{0} ant-tour))
		(is (some #{1} ant-tour))
		(is (some #{2} ant-tour))
		(is (= 0 (first ant-tour)))))

(deftest test-ant-walk-tour
	(let [[ant-tour-length tour] (ant-walk-tour (initialize-leg-data cities-on-map) cities-on-map)]
		(println ant-tour-length ":" tour)
		(is (some #{0} tour))
		(is (some #{1} tour))
		(is (some #{2} tour))
		(is (= 0 (first tour)))))

; (deftest test-multiple-ants-tour
; 	(let [shortest-ant-tour (multiple-ants-tour 10 cities-on-map)
; 		  [tour tour-length] shortest-ant-tour]
; 		  (println "tour" tour ":" tour-length)))

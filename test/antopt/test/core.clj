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

(deftest test-length-of-connection 
	(is (= 0.0 (length-of-connection  [0 1] [[0 0] [0 0]])))
	(is (= 5.0 (length-of-connection  [0 1] [[0 0] [4 3]]))))

(deftest test-length-of-tour
	(is (= 0 (length-of-tour [0] [0 0])))
	(is (= 10.0 (length-of-tour [0 1] [[0 0] [4 3]])))
	(is (= 10.0 (length-of-tour [0 2 1] [[0 0] [0 0] [4 3]]))))

(deftest test-create-connection-data 
	(let [test-info (create-connection-data [0 1] [[0 0] [4 3]])]
		(is (= 5.0 (:distance test-info)))
		(is (= 25.0 (:weighted-distance test-info)))
		(is (> 0.1 (:tau test-info))))) 

(deftest test-initialize-all-connections 
	(let [connections (initialize-all-connections [[0 0] [4 3]])
		test-info1 (connections [0 1])
        test-info2 (connections [1 0])]
        (is (= 5.0 (:distance test-info1)))
        (is (= 5.0 (:distance test-info2)))
        (is (= 25.0 (:weighted-distance test-info1)))
        (is (= 25.0 (:weighted-distance test-info2)))
        (is (= (:distance test-info1) (:distance test-info2)))
        (is (= (:weighted-distance test-info1) (:weighted-distance test-info2)))))

(deftest test-evaporate-connection-data 
    (let [test-connection (test-data [0 1])
         test-evap-data (evaporate-connection-data [0 1] test-connection)
         test-evap (test-evap-data [0 1])]
        (is (= (:distance test-evap) (:distance test-connection)))
        (is (< (:tau test-evap) (:tau test-connection)))
        (is (< (:weighted-tau test-evap) (:weighted-tau test-connection)))
        (is (= (:weighted-distance test-evap) (:weighted-distance test-connection)))
        (is (< (:probability test-evap) (:probability test-connection)))))

(deftest test-evaporate-pheromone
    (let [test-evap-data (evaporate-all-connections test-data)
	    test-connection1 (test-data [0 1])
	    test-evap1 (test-evap-data [0 1])
	    test-connection2 (test-data [1 0])
	    test-evap2 (test-evap-data [1 0])]
	    (is (= (:distance test-evap1) (:distance test-connection1)))
	    (is (< (:tau test-evap1) (:tau test-connection1)))
	    (is (< (:weighted-tau test-evap1) (:weighted-tau test-connection1)))
	    (is (= (:weighted-distance test-evap1) (:weighted-distance test-connection1)))
	    (is (< (:probability test-evap1) (:probability test-connection1)))
	    (is (= (:distance test-evap2) (:distance test-connection2)))
	    (is (< (:tau test-evap2) (:tau test-connection2)))
	    (is (< (:weighted-tau test-evap2) (:weighted-tau test-connection2)))
	    (is (= (:weighted-distance test-evap2) (:weighted-distance test-connection2)))
	    (is (< (:probability test-evap2) (:probability test-connection2)))))

(deftest test-adjust-pheromone-for-tour
    (let [evap-data (adjust-pheromone-for-tour test-data [10 [0 1 2]])
        test-connection1 (test-data [0 1])
        test-connection2 (test-data [1 2])
        test-evap1 (evap-data [0 1])
        test-evap2 (evap-data [1 2])]
        (is (= (:distance test-evap1) (:distance test-connection1)))
        (is (> (:tau test-evap1) (:tau test-connection1)))
        (is (> (:weighted-tau test-evap1) (:weighted-tau test-connection1)))
        (is (= (:weighted-distance test-evap1) (:weighted-distance test-connection1)))
        (is (> (:probability test-evap1) (:probability test-connection1)))
        (is (= (:distance test-evap2) (:distance test-connection2)))
        (is (> (:tau test-evap2) (:tau test-connection2)))
        (is (> (:weighted-tau test-evap2) (:weighted-tau test-connection2)))
        (is (= (:weighted-distance test-evap2) (:weighted-distance test-connection2)))
        (is (> (:probability test-evap2) (:probability test-connection2)))))

(deftest test-adjust-pheromone-for-multiple-tours
    (let [evap-data (adjust-pheromone-for-multiple-tours test-data [[10 [0 1 2]] [5 [2 1 0]]])
        test-connection1 (test-data [0 1])
        test-connection2 (test-data [1 2])
        test-evap1 (evap-data [0 1])
        test-evap2 (evap-data [1 2])]
        (is (= (:distance test-evap1) (:distance test-connection1)))
        (is (> (:tau test-evap1) (:tau test-connection1)))
        (is (> (:weighted-tau test-evap1) (:weighted-tau test-connection1)))
        (is (= (:weighted-distance test-evap1) (:weighted-distance test-connection1)))
        (is (> (:probability test-evap1) (:probability test-connection1)))
        (is (= (:distance test-evap2) (:distance test-connection2)))
        (is (> (:tau test-evap2) (:tau test-connection2)))
        (is (> (:weighted-tau test-evap2) (:weighted-tau test-connection2)))
        (is (= (:weighted-distance test-evap2) (:weighted-distance test-connection2)))
        (is (> (:probability test-evap2) (:probability test-connection2)))))

(deftest test-choose-next-city
    (let [next-city1 (choose-next-city test-data 0 [1 2])
    	next-city2 (choose-next-city test-data 0 [])]
        (is (some #{next-city1} [1 2]))
        (is (= next-city2 0))))

(deftest test-walk-ant-tour
    (let [[ant-length-of-tour tour] (walk-ant-tour (initialize-all-connections cities-on-map) cities-on-map)]
;       (println ant-length-of-tour ":" tour)
        (is (= (count tour) (count cities-on-map)))
        (is (= (count tour) (count (set tour))))
        (is (some #{0} tour))
        (is (some #{1} tour))
        (is (some #{2} tour))
        (is (= 0 (first tour)))))

(deftest test-one-generation-ant-tours 
        (let [foo (one-generation-ant-tours (initialize-all-connections cities-on-map) 5 cities-on-map)]
        (is (= 1 1))))

;(deftest test-antopt 
;        (let [foo (antopt)]
;        (is (= 1 1))))


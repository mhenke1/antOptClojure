(ns antopt.test.core
  (:use antopt.core clojure.test)
  (:import [antopt.core ConnectionInfo]))

(def nodes [
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

(def test-data {
                [2 1] (->ConnectionInfo 5 25 0.019983426066513734 7.993370426605494E-4), 
                [1 2] (->ConnectionInfo 5 25 0.08945806419434195  0.003578322567773678), 
                [1 0] (->ConnectionInfo 5 25 0.09253302650638885  0.003701321060255554), 
                [0 1] (->ConnectionInfo 5 25 0.049126426838469066 0.00196505707353876),
                [0 2] (->ConnectionInfo 5 25 0.055126426838469066 0.002365057073538763)})

(def test-distance-data {[2 1] 5, [1 2] 5, [1 0] 5, [0 1] 5, [0 2] 5})

(deftest test-euclidean-distance
  (is (= 0.0 (euclidean-distance [0 0] [0 0])))
  (is (= 5.0 (euclidean-distance [0 0] [4 3]))))

(deftest test-length-of-connection 
  (is (= 0 (length-of-connection  [0 1] [[0 0] [0 0]])))
  (is (= 5 (length-of-connection  [0 1] [[0 0] [4 3]])))
  (is (= 0 (length-of-connection  [1 1] [[0 0] [4 3]]))))

(deftest test-length-of-tour
  (is (= nil (length-of-tour test-distance-data [0 0])))
  (is (= 10 (length-of-tour test-distance-data [0 1 0])))
  (is (= 15 (length-of-tour test-distance-data [0 2 1 0]))))

(deftest test-create-connection-data 
  (let [test-connection-data (create-connection-data [0 1] [[0 0] [4 3]])
        test-info (test-connection-data [0 1])]
    (is (= 5 (:distance test-info)))
    (is (= 25.0 (:weighted-distance test-info)))
    (is (> 0.1 (:tau test-info))))) 

(deftest test-extract-distance-data
  (let [test-new-distance-data (extract-distance-data test-data)] 
    (is (= test-distance-data test-new-distance-data)))) 

(deftest test-initialize-connections 
  (let [connections (initialize-connections [[0 0] [4 3]])
        test-info1 (connections [0 1])
        test-info2 (connections [1 0])]
    (is (> 0.1 (:tau test-info1)))
    (is (> 0.1 (:tau test-info2)))
    (is (= 25.0 (:weighted-distance test-info1)))
    (is (= 25.0 (:weighted-distance test-info2)))
    (is (= 5 (:distance test-info1)))
    (is (= 5 (:distance test-info1)))))

(deftest test-evaporate-one-connection 
  (let [test-connection (test-data [0 1])
        test-evap (evaporate-one-connection test-connection)]
    (is (< (:tau test-evap) (:tau test-connection)))
    (is (< (:probability test-evap) (:probability test-connection)))))

(deftest test-evaporate-pheromone
  (let [test-evap-data (evaporate-all-connections test-data)
        test-connection1 (test-data [0 1])
        test-evap1 (test-evap-data [0 1])
        test-connection2 (test-data [1 0])
        test-evap2 (test-evap-data [1 0])]
    (is (< (:tau test-evap1) (:tau test-connection1)))
    (is (< (:probability test-evap1) (:probability test-connection1)))
    (is (< (:tau test-evap2) (:tau test-connection2)))
    (is (< (:probability test-evap2) (:probability test-connection2)))))

(deftest test-adjust-pheromone-for-one-connection
  (let [evap-data (adjust-pheromone-for-one-connection 10 test-data [0 1])
        test-connection (test-data [0 1])
        test-evap (evap-data [0 1])]
    (is (= (:distance test-evap) (:distance test-connection)))
    (is (= (:weighted-distance test-evap) (:weighted-distance test-connection)))
    (is (> (:tau test-evap) (:tau test-connection)))
    (is (> (:probability test-evap) (:probability test-connection)))))

(deftest test-adjust-pheromone-for-tour
  (let [evap-data (adjust-pheromone-for-tour test-data {:tour-length 10 :tour [0 1 2]})
        test-connection1 (test-data [0 1])
        test-connection2 (test-data [1 2])
        test-evap1 (evap-data [0 1])
        test-evap2 (evap-data [1 2])]
    (is (= (:distance test-evap1) (:distance test-connection1)))
    (is (= (:weighted-distance test-evap1) (:weighted-distance test-connection1)))
    (is (> (:tau test-evap1) (:tau test-connection1)))
    (is (> (:probability test-evap1) (:probability test-connection1)))
    (is (= (:distance test-evap2) (:distance test-connection2)))
    (is (= (:weighted-distance test-evap2) (:weighted-distance test-connection2)))
    (is (> (:tau test-evap2) (:tau test-connection2)))
    (is (> (:probability test-evap2) (:probability test-connection2)))))

(deftest test-adjust-pheromone-for-multiple-tours
  (let [evap-data (adjust-pheromone-for-multiple-tours test-data [{:tour-length 10 :tour [0 1 2]} {:tour-length 5 :tour [2 1 0]}])
        test-connection1 (test-data [0 1])
        test-connection2 (test-data [1 2])
        test-evap1 (evap-data [0 1])
        test-evap2 (evap-data [1 2])]
    (is (= (:distance test-evap1) (:distance test-connection1)))
    (is (= (:weighted-distance test-evap1) (:weighted-distance test-connection1)))
    (is (> (:tau test-evap1) (:tau test-connection1)))
    (is (> (:probability test-evap1) (:probability test-connection1)))
    (is (= (:distance test-evap2) (:distance test-connection2)))
    (is (= (:weighted-distance test-evap2) (:weighted-distance test-connection2)))
    (is (> (:tau test-evap2) (:tau test-connection2)))
    (is (> (:probability test-evap2) (:probability test-connection2)))))

(deftest test-choose-next-node-on-tour
  (let [next-node1 (choose-next-node-on-tour test-data 0 [1 2])]
    (is (some #{next-node1} [1 2]))))

(deftest test-add-next-node-to-tour
  (let [tour-and-new-remaining-nodes (add-next-node-to-tour test-data {:tour[0] :remaining-nodes [1 2]})
        {:keys [tour remaining-nodes]} tour-and-new-remaining-nodes
        next-node (peek tour)]
    (is (some #{next-node} [1 2]))
    (is (complement (some #{next-node} remaining-nodes)))))

(deftest test-walk-ant-tour
  (let [connection-data (initialize-connections nodes)
        distance-data (extract-distance-data connection-data)
        {:keys [tour-length tour]} (walk-ant-tour distance-data connection-data (count nodes))]
    (is (= (count tour) (+ 1 (count nodes))))
    (is (= (count tour) (+ 1 (count (set tour)))))
    (is (some #{0} tour))
    (is (some #{1} tour))
    (is (some #{2} tour))
    (is (= 0 (first tour))
        (is (> tour-length 0)))))

(deftest test-one-generation-ant-tours 
  (let [connection-data (initialize-connections nodes)
        foo (one-generation-ant-tours 5 (count nodes) (extract-distance-data connection-data) connection-data 1)]
    (is (= 1 1))))

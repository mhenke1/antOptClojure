(ns antopt.core-test
  "Tests for ant colony optimization core functionality."
  (:require [clojure.test :refer [deftest is testing]]
            [antopt.core :as aco]))

;; Test data
(def test-nodes
  [[37 52] [49 49] [52 64] [20 26] [40 30] [21 47]
   [17 63] [31 62] [52 33] [51 21] [42 41] [31 32]
   [5 25] [12 42] [36 16] [52 41] [27 23] [17 33]
   [13 13] [57 58] [62 42] [42 57] [16 57] [8 52]
   [7 38] [27 68] [30 48] [43 67] [58 48] [58 27]
   [37 69] [38 46] [46 10] [61 33] [62 63] [63 69]
   [32 22] [45 35] [59 15] [5 6] [10 17] [21 10]
   [5 64] [30 15] [39 10] [32 39] [25 32] [25 55]
   [48 28] [56 37] [30 40]])

(def simple-nodes
  [[0 0] [4 3] [8 0]])

(def test-config
  {:alpha 1.0
   :beta 2.0
   :rho 0.25
   :num-ants 10
   :num-generations 5})

;; Distance tests
(deftest euclidean-distance-test
  (testing "Euclidean distance calculation"
    (is (= 0.0 (aco/euclidean-distance [0 0] [0 0])))
    (is (= 5.0 (aco/euclidean-distance [0 0] [4 3])))
    (is (= 5.0 (aco/euclidean-distance [4 3] [0 0])))))

(deftest connection-distance-test
  (testing "Connection distance calculation"
    (is (= 0 (aco/connection-distance 0 0 simple-nodes)))
    (is (= 5 (aco/connection-distance 0 1 simple-nodes)))
    (is (= 5 (aco/connection-distance 1 0 simple-nodes)))))

(deftest tour-length-test
  (testing "Tour length calculation"
    (let [distances {[0 1] 5 [1 0] 5 [0 2] 8 [2 0] 8 [1 2] 5 [2 1] 5}]
      (is (= 10 (aco/tour-length distances [0 1 0])))
      (is (= 18 (aco/tour-length distances [0 1 2 0]))))))

;; Connection initialization tests
(deftest init-connection-test
  (testing "Connection initialization"
    (let [conn (aco/init-connection 0 1 simple-nodes test-config)
          [conn-id info] (first conn)]
      (is (= [0 1] conn-id))
      (is (= 5 (:distance info)))
      (is (= 25.0 (:weighted-distance info)))
      (is (< (:tau info) 0.1))
      (is (pos? (:probability info))))))

(deftest init-connections-test
  (testing "Initialize all connections"
    (let [connections (aco/init-connections simple-nodes test-config)]
      (is (= 6 (count connections)))  ; 3 nodes, 3*2 directed connections
      (is (contains? connections [0 1]))
      (is (contains? connections [1 0]))
      (is (contains? connections [0 2]))
      (is (contains? connections [2 0]))
      (is (contains? connections [1 2]))
      (is (contains? connections [2 1])))))

(deftest extract-distances-test
  (testing "Extract distance map from connections"
    (let [connections (aco/init-connections simple-nodes test-config)
          distances (aco/extract-distances connections)]
      (is (= 6 (count distances)))
      (is (= 5 (distances [0 1])))
      (is (= 5 (distances [1 0]))))))

;; Pheromone tests
(deftest evaporate-connection-test
  (testing "Pheromone evaporation on single connection"
    (let [info (aco/->ConnectionInfo 5 25.0 0.05 0.002)
          evaporated (aco/evaporate-connection info 0.25 1.0)]
      (is (< (:tau evaporated) (:tau info)))
      (is (< (:probability evaporated) (:probability info)))
      (is (= (:distance evaporated) (:distance info))))))

(deftest evaporate-all-test
  (testing "Pheromone evaporation on all connections"
    (let [connections (aco/init-connections simple-nodes test-config)
          original-tau (-> connections (get [0 1]) :tau)
          evaporated (aco/evaporate-all connections test-config)
          new-tau (-> evaporated (get [0 1]) :tau)]
      (is (< new-tau original-tau)))))

(deftest deposit-pheromone-test
  (testing "Pheromone deposit on single connection"
    (let [connections (aco/init-connections simple-nodes test-config)
          original-tau (-> connections (get [0 1]) :tau)
          updated (aco/deposit-pheromone connections [0 1] 100 test-config)
          new-tau (-> updated (get [0 1]) :tau)]
      (is (> new-tau original-tau)))))

(deftest deposit-tour-pheromone-test
  (testing "Pheromone deposit along tour"
    (let [connections (aco/init-connections simple-nodes test-config)
          tour {:length 18 :path [0 1 2 0]}
          original-tau-01 (-> connections (get [0 1]) :tau)
          original-tau-12 (-> connections (get [1 2]) :tau)
          updated (aco/deposit-tour-pheromone connections tour test-config)
          new-tau-01 (-> updated (get [0 1]) :tau)
          new-tau-12 (-> updated (get [1 2]) :tau)]
      (is (> new-tau-01 original-tau-01))
      (is (> new-tau-12 original-tau-12)))))

;; Tour construction tests
(deftest select-next-node-test
  (testing "Node selection from remaining nodes"
    (let [connections (aco/init-connections simple-nodes test-config)
          next-node (aco/select-next-node connections 0 [1 2])]
      (is (contains? #{1 2} next-node)))))

(deftest build-tour-test
  (testing "Complete tour construction"
    (let [connections (aco/init-connections simple-nodes test-config)
          tour (aco/build-tour connections 3)]
      (is (= 4 (count tour)))  ; 3 nodes + return to start
      (is (= 0 (first tour)))
      (is (= 0 (last tour)))
      (is (= 3 (count (set tour))))  ; 3 unique nodes (0 appears twice)
      (is (contains? (set tour) 0))
      (is (contains? (set tour) 1))
      (is (contains? (set tour) 2)))))

(deftest walk-ant-test
  (testing "Ant walk with tour length calculation"
    (let [connections (aco/init-connections simple-nodes test-config)
          distances (aco/extract-distances connections)
          result (aco/walk-ant distances connections 3)]
      (is (map? result))
      (is (contains? result :length))
      (is (contains? result :path))
      (is (pos? (:length result)))
      (is (= 4 (count (:path result)))))))

;; Integration tests
(deftest process-generation-test
  (testing "Process one generation of ants"
    (let [connections (aco/init-connections simple-nodes test-config)
          distances (aco/extract-distances connections)
          state (aco/create-state)
          result (aco/process-generation state connections distances test-config)]
      (is (map? result))
      (is (contains? result :state))
      (is (contains? result :connections))
      (is (= 1 (-> result :state :generation)))
      (is (< (-> result :state :shortest-tour :length) Long/MAX_VALUE)))))

(deftest optimize-test
  (testing "Full optimization run"
    (let [config (assoc test-config :num-generations 3)
          result (aco/optimize simple-nodes config)]
      (is (map? result))
      (is (contains? result :length))
      (is (contains? result :path))
      (is (pos? (:length result)))
      (is (= 4 (count (:path result))))
      (is (= 0 (first (:path result))))
      (is (= 0 (last (:path result)))))))

(deftest optimize-larger-instance-test
  (testing "Optimization on larger instance"
    (let [config (assoc test-config :num-generations 2 :num-ants 5)
          result (aco/optimize test-nodes config)]
      (is (map? result))
      (is (pos? (:length result)))
      (is (= (inc (count test-nodes)) (count (:path result))))
      (is (= 0 (first (:path result))))
      (is (= 0 (last (:path result)))))))

;; Made with Bob

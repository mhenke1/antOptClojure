(ns antopt.test.core
  (:use [antopt.core])
  (:use [clojure.test]))

(deftest test-distance
	(is (= 0.0 (distance [0 0] [0 0])))
	(is (= 5.0 (distance [0 0] [4 3]))))

(deftest test-leg-distance
	(is (= 0.0 (leg-distance [[0 0] [0 0]])))
	(is (= 5.0 (leg-distance [[0 0] [4 3]]))))

(deftest test-tour-length
	(is (= 0 (tour-length [0] [0 0])))
	(is (= 5.0 (tour-length [0 1] [[0 0] [4 3]])))
	(is (= 10.0 (tour-length [0 2 1] [[0 0] [0 0] [4 3]]))))

(deftest test-create-leg-info 
	(let [test-info (create-leg-info [0 1] [[0 0] [4 3]])]
	(is (= 5.0 (:distance test-info)))
	(is (= 25.0 (:weighted-distance test-info))))) 

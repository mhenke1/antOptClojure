(ns antopt.ui-test
  "Tests for UI helper functions."
  (:require [clojure.test :refer [deftest is testing]]
            [antopt.ui :as ui]))

;; Test data
(def test-nodes
  [[0 0] [10 10] [20 5]])

;; Coordinate scaling tests
(deftest scale-coordinates-test
  (testing "Coordinate scaling for display"
    (is (= [20 20] (ui/scale-coordinates [0 0])))
    (is (= [70 70] (ui/scale-coordinates [10 10])))
    (is (= [120 45] (ui/scale-coordinates [20 5])))))

;; Note: Functions that depend on Quil graphics context (draw-node, draw-connection, etc.)
;; are not unit tested here as they require a running Quil sketch.
;; These are tested through integration testing when running the UI.

;; Made with Bob

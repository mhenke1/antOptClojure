(ns antopt.cli-test
  "Tests for CLI utilities."
  (:require [clojure.test :refer [deftest testing is]]
            [antopt.cli :as cli]))

(deftest file-exists-test
  (testing "File existence check"
    (testing "Existing file returns true"
      (is (true? (cli/file-exists? "resources/eil51.tsm"))))
    
    (testing "Non-existing file returns false"
      (is (false? (cli/file-exists? "nonexistent.tsm"))))))

(deftest make-cli-options-test
  (testing "CLI options creation"
    (let [options (cli/make-cli-options "resources/bier127.tsm" 500 125)]
      
      (testing "Returns vector of option specs"
        (is (vector? options))
        (is (= 4 (count options))))
      
      (testing "File option spec"
        (let [file-opt (first options)
              option-map (apply hash-map (drop 3 file-opt))]
          (is (= "-f" (first file-opt)))
          (is (= "--file PATH" (second file-opt)))
          (is (= "Path to TSM file" (nth file-opt 2)))
          (is (= "resources/bier127.tsm" (:default option-map)))
          (is (fn? (first (:validate option-map))))))
      
      (testing "Ants option spec"
        (let [ants-opt (second options)
              option-map (apply hash-map (drop 3 ants-opt))]
          (is (= "-a" (first ants-opt)))
          (is (= "--ants N" (second ants-opt)))
          (is (= "Number of ants per generation" (nth ants-opt 2)))
          (is (= 500 (:default option-map)))
          (is (fn? (:parse-fn option-map)))
          (is (fn? (first (:validate option-map))))))
      
      (testing "Generations option spec"
        (let [gen-opt (nth options 2)
              option-map (apply hash-map (drop 3 gen-opt))]
          (is (= "-g" (first gen-opt)))
          (is (= "--generations N" (second gen-opt)))
          (is (= "Number of generations to run" (nth gen-opt 2)))
          (is (= 125 (:default option-map)))
          (is (fn? (:parse-fn option-map)))
          (is (fn? (first (:validate option-map))))))
      
      (testing "Help option spec"
        (let [help-opt (nth options 3)]
          (is (= "-h" (first help-opt)))
          (is (= "--help" (second help-opt)))
          (is (= "Show this help message" (nth help-opt 2))))))))

(deftest cli-options-with-different-defaults-test
  (testing "CLI options with custom defaults"
    (let [options (cli/make-cli-options "custom.tsm" 100 50)]
      
      (testing "Custom file default"
        (let [file-opt (first options)
              option-map (apply hash-map (drop 3 file-opt))]
          (is (= "custom.tsm" (:default option-map)))))
      
      (testing "Custom ants default"
        (let [ants-opt (second options)
              option-map (apply hash-map (drop 3 ants-opt))]
          (is (= 100 (:default option-map)))))
      
      (testing "Custom generations default"
        (let [gen-opt (nth options 2)
              option-map (apply hash-map (drop 3 gen-opt))]
          (is (= 50 (:default option-map))))))))

;; Made with Bob

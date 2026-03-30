#!/usr/bin/env bb
;; Simple test runner that can be executed with babashka or directly with java

(require '[clojure.test :as test])

;; Add src and test to classpath
(System/setProperty "java.class.path"
                    (str (System/getProperty "java.class.path")
                         ":src:test"))

;; Load test namespace
(require 'antopt.core-test)

;; Run tests
(let [results (test/run-tests 'antopt.core-test)]
  (System/exit (if (test/successful? results) 0 1)))

;; Made with Bob

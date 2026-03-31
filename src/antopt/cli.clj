(ns antopt.cli
  "Shared CLI utilities for argument parsing."
  (:require [clojure.java.io :as io]))

(defn file-exists?
  "Check if a file exists."
  [path]
  (.exists (io/file path)))

(defn make-cli-options
  "Create CLI options specification with the given default file path.
   
   Parameters:
   - default-file: Default TSM file path
   - default-ants: Default number of ants per generation
   - default-generations: Default number of generations
   
   Returns: Vector of CLI option specifications for tools.cli"
  [default-file default-ants default-generations]
  [["-f" "--file PATH" "Path to TSM file"
    :default default-file
    :validate [file-exists? "File must exist"]]
   ["-a" "--ants N" "Number of ants per generation"
    :default default-ants
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be a positive number"]]
   ["-g" "--generations N" "Number of generations to run"
    :default default-generations
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be a positive number"]]
   ["-h" "--help" "Show this help message"]])

;; Made with Bob

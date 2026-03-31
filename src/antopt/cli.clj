(ns antopt.cli
  "Shared CLI utilities for argument parsing."
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]))

(defn file-exists?
  "Check if a file exists at the given path.
   
   Parameters:
   - path: String path to the file
   
   Returns: Boolean indicating if file exists"
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
    :parse-fn #(try (Integer/parseInt %) (catch NumberFormatException _ nil))
    :validate [pos? "Must be a positive number"]]
   ["-g" "--generations N" "Number of generations to run"
    :default default-generations
    :parse-fn #(try (Integer/parseInt %) (catch NumberFormatException _ nil))
    :validate [pos? "Must be a positive number"]]
   ["-h" "--help" "Show this help message"]])

(defn handle-cli
  "Handle CLI parsing and execution with common error handling.
   
   Parameters:
   - args: Command line arguments vector
   - cli-options: CLI options specification
   - app-name: Application name for help display
   - run-fn: Function to execute with parsed options (receives options map)
   
   Returns: Exit code (0 for success, 1 for error)
   
   The run-fn should:
   - Accept a map with :file, :ants, and :generations keys
   - Throw exceptions on failure (FileNotFoundException, Exception, etc.)
   - Return normally on success (return value is ignored)
   
   Error handling:
   - Catches FileNotFoundException and displays user-friendly message
   - Catches general Exception and displays error message
   - Returns exit code 1 for any errors, 0 for success"
  [args cli-options app-name run-fn]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      ;; Show help
      (:help options)
      (do
        (println app-name)
        (println)
        (println summary)
        0)
      
      ;; Handle errors
      errors
      (do
        (println "Error parsing arguments:")
        (doseq [error errors]
          (println " " error))
        (println)
        (println summary)
        1)
      
      ;; Run application
      :else
      (try
        (run-fn options)
        0
        (catch java.io.FileNotFoundException e
          (println (str "Error: File not found: " (.getMessage e)))
          1)
        (catch Exception e
          (println (str "Error: " (.getMessage e)))
          1)))))

;; Made with Bob

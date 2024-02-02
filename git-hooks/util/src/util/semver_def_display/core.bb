#!/usr/bin/env bb

;; (c) Copyright 2023-2024 KineticFire. All rights reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.


;; KineticFire Labs
;;	  Project site:  https://github.com/kineticfire-labs/semver-multi



(ns util.semver-def-display.core
  (:require [clojure.string    :as str]
            [babashka.process  :refer [shell]]
            [clojure.java.io   :as io]
            [cheshire.core     :as json]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")


(def ^:const default-config-file "project.def.json")

(def ^:const indent-amount 2)


(defn ^:impure handle-ok
  "Exits with exit code 0."
  []
  (common/exit-now! 0))


(defn ^:impure handle-err
  "Displays message `msg` using shell and then exits with exit code 1."
  [msg]
  (common/run-shell-command (common/apply-display-with-shell msg))
  (common/exit-now! 1))


(defn ^:impure handle-warn
  "Displays a warning message `msg`."
  [msg]
  (common/run-shell-command (common/apply-display-with-shell msg)))



(comment (defn process-main-with-valid-inputs
  [cli-args config]
  (let [config-enabled (common/config-enabled? config)]
    (when (not config-enabled)
      (handle-warn (str "\"" common/shell-color-yellow "WARNING: Config disabled!" "\"")))
    (println "other stuff")
    (let [options (prepare-options cli-args config)]
      (if (or (nil? options)
              (:success options))
        (println (select-keys options [:scope-path :json-path])) ;; todo: resume here.  if no options, then map is empty.
        (handle-err (str "\"" common/shell-color-red "Could not find query path for '" (first cli-args) "'\""))))
    (when (not config-enabled)
      (handle-warn (str "\"" common/shell-color-yellow "WARNING: Config disabled!" "\""))))))


(defn process-options-f
  "Processes options with the '-f' flag and assigns the value of the flag to ':config-file'."
  [response defined args]
  (let [arg (first (rest args))
        args (rest (rest args))]
    (if (nil? arg)
      {:success false
       :reason "Flag '-f' must be followed by a config file path."}
      (if (some (fn [itm] (= :config-file itm)) defined)
        {:success false
         :reason "Duplicate definition of config file."}
        {:success true
         :response (assoc response :config-file arg)
         :defined (conj defined :config-file)
         :args args}))))


(defn process-options-default
  "Processes default options, e.g. those options without a flag.  Currenty handles only the default option of 'alias-scope-path'.  Takes the first element of `args` and assigns that as the value to the key ':alias-scope-path' in the response then updates `defined` with ':alias-scope-path' to indicate it was handled and updates `args` by removing the first element; adds 'success' to boolean 'true'.  If unsuccessful, returns key 'success' to boolean 'false'."
  [response defined args]
  (if (some (fn [itm] (= :alias-scope-path itm)) defined)
    {:success false
     :reason "Duplicate definition of alias scope path."}
    {:success true
     :response (assoc response :alias-scope-path (first args))
     :defined (conj defined :alias-scope-path)
     :args (rest args)}))


(defn process-options
  "Processes CLI options `cli-args`, returning key 'success' set to boolean true with found options and boolean 'false' otherwise with 'reason' set to string reason.  The `config` must be valid.  On success, found options may include:  'config-file' (override default config file path/name), as a pair 'json-path' and 'scope-path' (query path for json through the config and scope path through the config, as desired by the user)"
  [cli-args default-config-file]
  (let [err-msg-pre "Invalid options format."
        err-msg-post "Usage:  semver-def-display <optional -f config file path> <optional scope path>"]
    (if (> (count cli-args) 3)
      {:success false
       :reason (str err-msg-pre " Zero to two arguments accepted. " err-msg-post)}
      (loop [response {:success true
                       :config-file default-config-file}
             defined []
             args cli-args]
        (if (empty? args)
          response
          (let [arg (first args)
                result (case arg
                         "-f" (process-options-f response defined args)
                         (process-options-default response defined args))]
            (if (not (:success result))
              (assoc result :reason (str err-msg-pre " " (:reason result) " " err-msg-post))
              (recur (:response result) (:defined result) (:args result)))))))))


(defn process-alias-scope-path
  "Updates and returns the `options` map based on its ':alias-scope-path', possibly using the `config`.  If no ':alias-scope-path' was set, `options` contains key 'success' to boolean 'true'.  If ':alias-scope-path' is set and is valid in the `config`, then adds to `options` key success to boolean 'true', 'scope-path' as a vector of strings of scopes (even if the ':alias-scope-path' contained scope aliases), and the 'json-path' as a vector of the json path (using keywords and integer indicies) through the config.  Else if invalid, then returns 'success' to boolean 'false', a 'reason' with a string reason, and 'locations' as a vector with element integer '0'. The `config` must be valid."
  [options config]
  (if (nil? (:alias-scope-path options))
    (assoc options :success true)
    (merge options (common/find-scope-path (:alias-scope-path options) config))))


(defn compute-display-config-node-header-format
  ([type]
   (compute-display-config-node-header-format type 0))
  ([type level]
   (let [indent (* level (* 2 indent-amount))]
     (case type
       :project (str (str/join (repeat indent ".")) "PROJECT")
       :projects (str (str/join (repeat indent ".")) "PROJECTS")
       :artifacts (str (str/join (repeat indent ".")) "ARTIFACTS")))))


(defn compute-display-config-node-header
  [output path level]
  (if (empty? path)
    output
    (if (= :project (nth path (dec (count path))))
      (conj output (compute-display-config-node-header-format :project))
      (if (= 0 (nth path (dec (count path))))
        (conj output (compute-display-config-node-header-format (nth path (- (count path) 2)) level))
        output))))


(defn compute-display-config-node-name-format
  [name level]
  (let [indent (+ (* level (* 2 indent-amount)) indent-amount)]
    (str (str/join (repeat indent ".")) name)))



(defn compute-display-config-node-name
  [output node level]
  (if (empty? node)
    output
    (let [name (common/get-name node)]
      (conj output (compute-display-config-node-name-format name level)))))



(defn compute-display-config-node-info-format
  [info level]
  (let [indent (+ (* level (* 2 indent-amount)) (* 2 indent-amount))]
    (str (str/join (repeat indent ".")) info)))



(defn compute-display-config-node-info
  [output node scope-path alias-path level detail]
  (if (empty? node)
    output
    (-> output
        (conj (compute-display-config-node-info-format (str "scope-path: " (str/join "." scope-path)) level))
        (conj (compute-display-config-node-info-format (str "alias-path: " (str/join "." alias-path)) level)))))


(defn get-child-nodes
  "Returns vector of child node descriptions (projects and/or artifacts) for the `node` or an empty vector if there a reno child nodes.  The child node descriptions are built from the `child-node-descr` and `parent-path`."
  [node child-node-descr parent-path]
  (into [] (reverse (concat
                     (map-indexed (fn [idx itm] (-> child-node-descr
                                                    (assoc :type :artifacts)
                                                    (assoc :path (conj parent-path :artifacts idx)))) (get-in node [:artifacts]))
                     (map-indexed (fn [idx itm] (-> child-node-descr
                                                    (assoc :type :projects)
                                                    (assoc :path (conj parent-path :projects idx)))) (get-in node [:projects]))))))


(defn compute-display-config-project
  [])


;; todo: for when an alias scope path is provided
(defn compute-display-config-path
  [json-path config]
  (if (nil? json-path)
    {}
    (do
      (println (compute-display-config-node-header :project 0))
      (println (compute-display-config-project [:project] [] [] 0 true config)))))


;;todo for using the alias scope path, starting in the 'compute-display-config'
(comment (let [{output :output node :node :or {output [] node [:project]}} (compute-display-config-path (:json-path options) config)] ;; output, node
           (println output node)))

;; in-order depth-first traversal
(defn compute-display-config
  [config options]
  (loop [prev-output []
         stack  [{:type :project
                  :path [:project]
                  :parent-scope-path []
                  :parent-alias-path []
                  :level 0
                  :detail true}]]
    (if (empty? stack)
      prev-output
      (let [node-descr (peek stack) 
            node (get-in config (:path node-descr))
            scope-path (conj (:parent-scope-path node-descr) (common/get-scope node))
            alias-path (conj (:parent-alias-path node-descr) (common/get-scope-alias-else-scope node))
            child-node-descr {:parent-scope-path scope-path
                              :parent-alias-path alias-path
                              :level (inc (:level node-descr))
                              :detail (:detail node-descr)}
            updated-output (-> prev-output
                               (compute-display-config-node-header (:path node-descr) (:level node-descr))
                               (compute-display-config-node-name node (:level node-descr))
                               (compute-display-config-node-info node scope-path alias-path (:level node-descr) (:detail node-descr)))]
        (recur updated-output (into [] (concat (pop stack) (get-child-nodes node child-node-descr (:path node-descr)))))))))


(defn display-output
  "Formats `output` and displays it to the shell with the 'echo' command.  Applies outer quotes and 'echo -e' command.  The argument `output` can be a String or sequence of Strings."
  [output]
  (common/run-shell-command (common/apply-display-with-shell (common/apply-quotes output))))


;; todo: for testing, can use: p.h.c.c
;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-main
  [cli-args default-config-file-path default-config-file-name]
  (let [options (process-options cli-args (str default-config-file-path "/" default-config-file-name))]
    (if (:success options)
      (let [config-file (:config-file options)
            config-parse-response (common/parse-json-file (:config-file options))]
        (if (:success config-parse-response)
          (let [config (:result config-parse-response)
                config-validate-response (common/validate-config config)]
            (if (:success config-validate-response)
              (let [alias-scope-path-response (process-alias-scope-path options config)]
                (if (:success alias-scope-path-response)
                  (let [enhanced-options (select-keys alias-scope-path-response [:config-file :alias-scope-path :scope-path :json-path])]
                    (compute-display-config config enhanced-options))
                  (handle-err (str "\"" common/shell-color-red "Error finding alias scope path of '" (:alias-scope-path options) "'. " (:reason alias-scope-path-response) "\""))))
              (handle-err (str "\"" common/shell-color-red "Error validating config file at " config-file ". " (:reason config-validate-response) "\""))))
          (handle-err (str "\"" common/shell-color-red "Error reading config file. " (:reason config-parse-response) "\""))))
      (handle-err (str "\"" common/shell-color-red "Error: " (:reason options) "\"")))))


(defn ^:impure -main
  "Validates the project config (defined as a constant) and formats/validates the commit edit message (provided as the function argument).  Returns exit value 0 (allowing the commit) if the message enforcement in the config disabled or if the config and commit message are valid; if message enforcement is enabled and the commit edit message is valid, then re-formats the commit edit message.  Returns exit value 1 (aborting the commit) if the config or edit message are invalid or other error occured.  One argument is required, which is the path to the commit edit message.
   
   The order of checks for validity are:
      - one arg required, which is path to the commit edit message file
         - exit 1 if not one arg
      - read/parse JSON config file
         - exit 1 if 
            - file doesn't exist or can't read file
            - JSON file fails to parse
      - validate config
         - exit 1 if config invalid
      - check config enabled
         - exit 0 if disabled
      - retrieve git edit message file
         - exit 1 if file doesn't exist or can't read file
      - format git edit message file
      - validate git edit message
         - exit 1 if invalid
      - write git edit message to file
         - exit 1 if fail
      - exit 0 (success)"
  [& args]
  (perform-main args (common/get-git-root-dir) default-config-file))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

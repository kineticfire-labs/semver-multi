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

;; tie between 'scope-path' and 'scope-alias'
(def ^:const longest-label-num-chars (count "scope-path"))



(defn get-highlight-code
  "Returns the shell color code for highlighting if boolean `highlight` if true else if false returns a non-highlight code."
  [highlight]
  (if highlight
    common/shell-color-red
    common/shell-color-white))


(defn display-output
  "Formats `output` and displays it to the shell with the 'echo' command.  Applies outer quotes and 'echo -e' command.  The argument `output` can be a String or sequence of Strings."
  [output]
  (common/run-shell-command (common/apply-display-with-shell (common/apply-quotes output))))


(defn ^:impure handle-ok
  "Exits with exit code 0."
  []
  (common/exit-now! 0))


(defn ^:impure handle-err
  "Displays message string `msg` using shell and then exits with exit code 1."
  [msg]
  (common/run-shell-command (common/apply-display-with-shell msg))
  (common/exit-now! 1))


(defn ^:impure handle-warn
  "Displays a warning message string `msg`."
  [msg]
  (common/run-shell-command (common/apply-display-with-shell msg)))


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
  "Processes CLI options map `cli-args`, returning a map with key 'success' set to boolean true with found options and boolean 'false' otherwise with 'reason' set to string reason.  The `config` must be valid.  On success, found options may include:  'config-file' (override default config file path/name of string `default-config-file`), as a pair 'json-path' and 'scope-path' (query path for json through the config and scope path through the config, as desired by the user)"
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
  "Updates and returns the `options` map based on its ':alias-scope-path', possibly using the map `config`.  If no ':alias-scope-path' was set, `options` contains key 'success' to boolean 'true'.  If ':alias-scope-path' is set and is valid in the `config`, then adds to `options` key success to boolean 'true', 'scope-path' as a vector of strings of scopes (even if the ':alias-scope-path' contained scope aliases), and the 'json-path' as a vector of the json path (using keywords and integer indicies) through the config.  Else if invalid, then returns 'success' to boolean 'false', a 'reason' with a string reason, and 'locations' as a vector with element integer '0'. The `config` must be valid."
  [options config]
  (if (nil? (:alias-scope-path options))
    (assoc options :success true)
    (merge options (common/find-scope-path (:alias-scope-path options) config))))


(defn compute-display-config-node-header-format
  "Returns a string for the header of the node, including a shell color highlight code if boolean `highlight` is true and a default color code otherwise.  If `highlight` only, then the header is assumed to be for a 'project'.  Otherwise keyword `type` should be be ':projects' or ':artifacts' and integer `level` is the level of node starting at zero.  Only adds the header if the node in projects or arfiacts is the first node."
  ([highlight]
   (compute-display-config-node-header-format :project 0 highlight))
  ([type level highlight]
   (let [indent (* level (* 2 indent-amount))]
     (case type
       :project (str (get-highlight-code highlight) (str/join (repeat indent " ")) "PROJECT----------"  common/shell-color-reset)
       :projects (str (get-highlight-code highlight)(str/join (repeat indent " ")) "PROJECTS---------"  common/shell-color-reset)
       :artifacts (str (get-highlight-code highlight) (str/join (repeat indent " ")) "ARTIFACTS---------"  common/shell-color-reset)))))


(defn compute-display-config-node-header
  "Computes the line, if any, for the header and returns vector `output`.  If no change, the `output` is returned unchanged.  The vector `path` is the json path that defines the location of the node, integer `level` is the level of the node (starting at 0), and boolean `highlight` adds shell color code if true else a default color."
  [output path level highlight]
  (if (empty? path)
    output
    (if (= :project (nth path (dec (count path))))
      (conj output (compute-display-config-node-header-format highlight))
      (if (= 0 (nth path (dec (count path))))
        (conj output (compute-display-config-node-header-format (nth path (- (count path) 2)) level highlight))
        output))))


(defn compute-display-config-node-name-format
  "Returns a string for the string `name` at integer `level` using shell color codes determined by boolean `highlight`."
  [name level highlight]
  (let [indent (+ (* level (* 2 indent-amount)) indent-amount)]
    (str (get-highlight-code highlight) (str/join (repeat indent " ")) name  common/shell-color-reset)))


(defn compute-display-config-node-name
  "Updates vector `output` with the node name or returns `output` unchanged if `node` is empty.  The `node` must be a valid map of the node, `level` is the level of the node starting at zero, and `highlight` adds a shell highlight code if true else a default color code."
  [output node level highlight]
  (if (empty? node)
    output
    (let [name (common/get-name node)]
      (conj output (compute-display-config-node-name-format name level highlight)))))


(defn compute-display-config-node-info-format
  "Returns a string for string `info` with prepended spaces, if any, necessary for the integer `level` starting at zero."
  [info level]
  (let [indent (+ (* level (* 2 indent-amount)) (* 2 indent-amount))]
    (str (str/join (repeat indent " ")) info)))


(defn add-if-defined
  "Updates vector `output` with the string `label` and its string value if the value at vector `path` in map `node` is not nil, else returns `output` unchanged.  If updating `output`, then adds a line '<label>: '<value>' where the spacing between `label` the colon is offset, if any, based on 'longest-label-num-chars' and uses shell color coding with highlight color if `highlight` is true else use a default color."
  [output node path label color level]
  (if (nil? (get-in node path))
    output
    (conj output (compute-display-config-node-info-format (str color label (str/join (repeat (- longest-label-num-chars (count label)) " ")) ": " (get-in node path) common/shell-color-reset) level))))


(defn add-if-defined-comma-sep
  "Updates vector `output` with the string `label` and its vector value if the value at vector `path` in map `node` is not nil, else returns `output` unchanged.  If updating `output`, then adds a line '<label>: '<value1>, <value2>, ...' where the spacing between `label` the colon is offset, if any, based on 'longest-label-num-chars' and uses shell color coding with highlight color if `highlight` is true else use a default color."
  [output node path label color level]
  (if (nil? (get-in node path))
    output
    (conj output (compute-display-config-node-info-format (str color label (str/join (repeat (- longest-label-num-chars (count label)) " ")) ": " (str/join ", " (get-in node path)) common/shell-color-reset) level))))


(defn compute-display-config-node-info
  "Updates vector `output` if map `node` is not empty otherwise return `output` unchanged.  Updates `output` for the node with its vector of strings `name-path`, description (if defined), includes (if defined), vector of strings scope-path, and vector of strings alias-path.  Adds spaces to added string based on integer `level` and include shell highlight color code if `highlight` is true else uses default color code."
  [output node name-path scope-path alias-path level highlight]
  (if (empty? node)
    output
    (let [color (get-highlight-code highlight)]
      (-> output
          (conj (compute-display-config-node-info-format (str color "name-path : " (str/join "." name-path) common/shell-color-reset) level))
          (add-if-defined node [:description] "descr" color level)
          (add-if-defined-comma-sep node [:includes] "includes" color level)
          (conj (compute-display-config-node-info-format (str color "scope-path: " (str/join "." scope-path) common/shell-color-reset) level))
          (conj (compute-display-config-node-info-format (str color "alias-path: " (str/join "." alias-path) common/shell-color-reset) level))))))

;; todo: need tests below
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

;; can't be nil, must be valid path
(defn build-queue-for-compute-display-config-path
  [json-path]
  (loop [queue [[(first json-path)]]
         json-path (rest json-path)]
    (if (empty? json-path)
      queue
      (recur (conj queue (-> (last queue)
                             (conj (nth json-path 0))
                             (conj (nth json-path 1)))) (rest (rest json-path))))))


;; config and json-path valid
(defn compute-display-config-path
  [config json-path]
  (if (nil? json-path)
    {:output []
     :stack [{:path [:project]
             :parent-name-path []
             :parent-scope-path []
             :parent-alias-path []
             :level 0}]}
    (loop [output []
           queue (build-queue-for-compute-display-config-path json-path)
           last-json-path []
           parent-name-path []
           parent-scope-path []
           parent-alias-path []
           level 0]
      (if (empty? queue)
        (let [child-node-descr {:parent-name-path parent-name-path
                                :parent-scope-path parent-scope-path
                                :parent-alias-path parent-alias-path
                                :level (inc level)}]
          {:output output
           :stack (get-child-nodes (get-in config last-json-path) child-node-descr last-json-path)})
        (let [path (first queue)
              node (get-in config path)
              name-path (conj parent-name-path (:name node))
              scope-path (conj parent-scope-path (common/get-scope node))
              alias-path (conj parent-alias-path (common/get-scope-alias-else-scope node))
              output (-> output
                         (compute-display-config-node-header path level true)
                         (compute-display-config-node-name node level true)
                         (compute-display-config-node-info node name-path scope-path alias-path level true))]
          (recur output (rest queue) path name-path scope-path alias-path (inc level)))))))


;; in-order depth-first traversal
(defn compute-display-config
  [config options]
  (let [ans (compute-display-config-path config (:json-path options))]
    (loop [prev-output (:output ans)
           stack (:stack ans)]
     (if (empty? stack)
       prev-output
       (let [node-descr (peek stack)
             node (get-in config (:path node-descr))
             name-path (conj (:parent-name-path node-descr) (:name node))
             scope-path (conj (:parent-scope-path node-descr) (common/get-scope node))
             alias-path (conj (:parent-alias-path node-descr) (common/get-scope-alias-else-scope node))
             child-node-descr {:parent-name-path name-path
                               :parent-scope-path scope-path
                               :parent-alias-path alias-path
                               :level (inc (:level node-descr))}
             updated-output (-> prev-output
                                (compute-display-config-node-header (:path node-descr) (:level node-descr) false)
                                (compute-display-config-node-name node (:level node-descr) false)
                                (compute-display-config-node-info node name-path scope-path alias-path (:level node-descr) false))]
         (recur updated-output (into [] (concat (pop stack) (get-child-nodes node child-node-descr (:path node-descr))))))))))


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
                    (display-output (compute-display-config config enhanced-options)))
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

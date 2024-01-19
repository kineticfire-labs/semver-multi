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


(defn handle-display
  [cli-args config]
  (let [config-enabled (common/config-enabled? config)]
    (when (not config-enabled)
      (println "WARNING: Config disabled"))
    (println "other stuff")
    (when (not config-enabled)
      (println "WARNING: Config disabled"))))


;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-display
  [cli-args config-file-path config-file-name]
  (if (< (count cli-args) 2)
    (if (some? config-file-path)
      (let [config-file (str config-file-path "/" config-file-name)
            config-parse-response (common/parse-json-file config-file)]
        (if (:success config-parse-response)
          (let [config (:result config-parse-response)
                config-validate-response (common/validate-config config)]
            (if (:success config-validate-response)
              (handle-display cli-args config)
              (println (str "Error validating config file at " config-file ". " (:reason config-validate-response)))))
          (println (str "Error reading config file. " (:reason config-parse-response)))))
      (println "Error reading config file.  Could not find git repository root."))
    (println "Error: zero or one arguments accepted.  Usage:  semver-def-display <optional scope path>")))

;; Commit message enforcement disabled.
;; if (common/config-enabled? config)



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
  (perform-display args (common/get-git-root-dir) default-config-file))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

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

;;
;; p.h.c.c
;;

;; todo: add to below a ':alias-scope-path' ?

;; common/find-scope-path
;; "Finds the scope and json paths for the string `query-path`, which can be a dot-separated path of scope and/or scope-aliases, using the `config` returning a map result.
;;
;; If found, returns
;; - 'success' to boolean 'true'
;; - 'scope-path' as a vector of strings of scopes (even if the `query-path` contained scope aliases)
;; - 'json-path' as a vector of the json path (using keywords and integer indicies) through the config.
;; If invalid
;; - 'success' to boolean 'false'
;; - 'reason' with a string reason
;; - 'locations' as vector with element 0.
;;
;; The `config` must be valid."


(defn process-options-f
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
  [cli-args config-file]
  (let [err-msg-pre "invalid options format."
        err-msg-post "Usage:  semver-def-display <optional -f config file path> <optional scope path>"]
    (if (> (count cli-args) 3)
      {:success false
       :reason (str err-msg-pre " Zero to two arguments accepted. " err-msg-post)}
      (loop [response {:success true
                       :config-file config-file}
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



;;(common/find-scope-path arg config)

;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-main
  [cli-args default-config-file-path default-config-file-name]
  ;;
  ;;
  (comment (if (< (count cli-args) 4)
    (if (some? config-file-path)
      (let [config-file (str config-file-path "/" config-file-name)
            config-parse-response (common/parse-json-file config-file)]
        (if (:success config-parse-response)
          (let [config (:result config-parse-response)
                config-validate-response (common/validate-config config)]
            (if (:success config-validate-response)
              (println "ok!")
              (handle-err (str "\"" common/shell-color-red "Error validating config file at " config-file ". " (:reason config-validate-response) "\""))))
          (handle-err (str "\"" common/shell-color-red "Error reading config file. " (:reason config-parse-response) "\""))))
      (handle-err (str "\"" common/shell-color-red "Error reading config file.  Could not find git repository root." "\"")))
    (handle-err (str "\"" common/shell-color-red "Error: zero or one arguments accepted.  Usage:  semver-def-display <optional -f config file path> <optional scope path>" "\""))))
  ;;
  ;;
  (let [options (process-options cli-args (str default-config-file-path "/" default-config-file-name))]
    (if (:success options)
      (println "OK!" options)
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

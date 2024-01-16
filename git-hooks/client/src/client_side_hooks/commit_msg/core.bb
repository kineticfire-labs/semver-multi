#!/usr/bin/env bb

;; (c) Copyright 2023 KineticFire. All rights reserved.
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



(ns client-side-hooks.commit-msg.core
  (:require [clojure.string    :as str]
            [babashka.process  :refer [shell]]
            [clojure.java.io   :as io]
            [cheshire.core     :as json]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")

;; todo changed path for testing
(def ^:const default-config-file "project.def.json")

(def ^:const title "by local commit-msg hook.")


;; todo tests
;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-check
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
  [args config-file]
  (if (= (count args) 1)
    (let [commit-msg-file (first args)
          config-parse-response (common/parse-json-file config-file)]
      (if (:success config-parse-response)
        (let [config (:result config-parse-response)
              config-validate-response (common/validate-config config)]
          (if (:success config-validate-response)
            (if (common/config-enabled? config)
              (let [commit-msg-read-response (common/read-file commit-msg-file)]
                (if (:success commit-msg-read-response)
                  (let [commit-msg-formatted (common/format-commit-msg (:result commit-msg-read-response))
                        commit-msg-validate-response (common/validate-commit-msg commit-msg-formatted config)]
                    (if (:success commit-msg-validate-response)
                      (let [write-response (common/write-file commit-msg-file "content")]
                        (if (:success write-response)
                          (common/handle-ok title)
                          (common/handle-err title (str "Commit message could not be written to commit message edit file '" commit-msg-file "'. " (:reason write-response)) commit-msg-formatted)))
                      (common/handle-err title (str "Commit message invalid '" commit-msg-file "'. " (:reason commit-msg-validate-response)) commit-msg-formatted (:locations commit-msg-validate-response))))
                  (common/handle-err title (str "Error reading git commit edit message file '" commit-msg-file "'. " (:reason commit-msg-read-response)))))
              (common/handle-warn-proceed title "Commit message enforcement disabled."))
            (common/handle-err title (str "Error validating config file at " config-file ". " (:reason config-validate-response)))))
        (common/handle-err title (str "Error reading config file. " (:reason config-parse-response)))))
    (common/handle-err title "Exactly one argument required.  Usage:  commit-msg <path to git edit message>")))


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
  (perform-check args default-config-file))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

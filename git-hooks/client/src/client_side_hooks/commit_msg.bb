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
;;	  Project site:  https://github.com/kineticfire-labs/git-conventional-commits-hooks



(ns client-side-hooks.commit-msg
  (:require [clojure.string    :as str]
            [babashka.cli      :as cli]
            [babashka.process  :refer [shell process exec]]
            [clojure.java.io   :as io]
            [cheshire.core     :as json]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")

;; todo changed path for testing
(def ^:const config-file "../resources/project-small.def.json")

(def ^:const title "by local commit-msg hook.")



;; todo for testing tests
(defn add-up
  [a b]
  (+ a b))
;;(println "add-up 1 2" (add-up 1 2))

;; todo for testing inclusion of 'common' project
;;(defn current-add-two
;;  [x]
;;  (common/add-two x))
;;(println "current-add-two 5" (current-add-two 5))




;; - one arg required, which is path to commit edit message file
;;    - exit 1 if not one arg given
;; - read/parse JSON config file
;;    - exit 1 if
;;      - file doesn't exist or can't read file
;;      - JSON file fails to parse
;; * validate config (todo)
;;    - exit 1 if config invalid
;; - check config enabled
;;    - exit 0 if
;;      - disabled
;; - retrieve git edit message file
;;    - exit 1 if
;;      - file doesn't exist or can't read file
;; - format git edit message file
;; * validate git commit message (todo... need validated config for defined types/scopes)
;; * write git edit message to file (todo)
;;   - exit 1 if fail
;; exit 0


;; One argument required, which is the path to the commit edit message.
(defn ^:impure -main
  "Validates the project config and formats/validates the commit edit message.  Returns exit value 0 (allowing the commit) if the message enforcement in the config disabled or if the config and commit message are valid.  Returns exit value 1 (aborting the commit) if the config or edit message are invalid or other error occured.  One argument required, which is the path to the commit edit message."
  [& args]
  (if (= (count args) 1)
    (let [config-parse-response (common/parse-json-file config-file)]
      (if (:success config-parse-response)
        (let [config (:result config-parse-response)
              config-validate-response (common/validate-config config)]
          (if (:success config-validate-response)
            (if (common/config-enabled? config)
              (let [commit-msg-read-response (common/read-file (first args))]
                (if (:success commit-msg-read-response)
                  (let [commit-msg-formatted (common/format-commit-msg (:result commit-msg-read-response))
                        commit-msg-validate-response (common/validate-commit-msg commit-msg-formatted config)]
                    (if (:success commit-msg-validate-response)
                      (println "commit msg valid!") ;;todo exit 0
                      (common/handle-err-exit title (str "Commit message invalid '" (first args) "'. " (:reason commit-msg-validate-response)) commit-msg-formatted (:locations commit-msg-validate-response))))
                  (common/handle-err-exit title (str "Error reading git commit edit message file '" (first args) "'. " (:reason commit-msg-read-response)))))
              (common/handle-warn-proceed title "Commit message enforcement disabled."))
            (common/handle-err-exit title (str "Error validating config file at " config-file "." (:reason config-validate-response)))))
        (common/handle-err-exit title (str "Error reading config file. " (:reason config-parse-response)))))
    (common/handle-err-exit title "Exactly one argument required.  Usage:  commit-msg <path to git edit message>")))


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

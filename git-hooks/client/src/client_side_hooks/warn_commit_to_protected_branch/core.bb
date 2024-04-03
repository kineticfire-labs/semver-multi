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


;; client-side 'pre-commit' git hook to warn on committing to a protected branch
;;   - parameters
;;      - none


(ns client-side-hooks.warn-commit-to-protected-branch.core
  (:require [clojure.string    :as str]
            [clojure.java.io   :as io]
            [babashka.process  :refer [shell]]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")

(def ^:const protected-branches ["main"])


(defn generate-warn-msg
  [branch]
  (common/apply-display-with-shell
   [(str "\"" common/shell-color-red "WARNING\"")
    (str "\"" common/shell-color-red "You are attempting to commit to '" branch "'." common/shell-color-reset "\"")]))


(defn generate-prompt-msg
  [branch]
  (common/apply-display-with-shell
   [(str "\"" common/shell-color-white "Type 'yes' if you wish to continue the commit to '" branch "'.  Any other input aborts the commit." common/shell-color-reset "\"")]))


;;todo - what is return value of :out when not in git repo?  assuming it's nil
(defn ^:impure get-git-branch
  "Returns the branch name active in the repo or 'nil' if the command was not executed in a git repo or the command
   failed."
  []
  (let [resp (-> (shell {:out :string :err :string} "git rev-parse --abbrev-ref HEAD")
                 (select-keys [:out :err]))]
    (if (nil? (:out resp))
      nil
      (str/trim (:out resp)))))


;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-warn
  ""
  [branches]
  (let [branch (get-git-branch)]
    (when (some #(= branch %) branches)
      (common/run-shell-command (generate-warn-msg branch))
      (common/run-shell-command (generate-prompt-msg branch))

      ;; todo prompt user to confirm 

      (common/exit-now! 1)
      )))


(defn ^:impure -main
  ""
  [& args]
  (perform-warn protected-branches))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

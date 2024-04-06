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


(ns client-side-hooks.warn-commit-branch.core
  (:require [clojure.string    :as str]
            [clojure.java.io   :as io]
            [babashka.process  :refer [shell]]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")

(def ^:const protected-branches ["main"])


(defn generate-warn-msg
  "Generates a warning message, including shell color-coding, about an attempt commit to 'branch'."
  [branch]
  (common/apply-display-with-shell
   [(str "\"" common/shell-color-red "WARNING\"")
    (str "\"" common/shell-color-red "You are attempting to commit to branch '" branch "'." common/shell-color-reset "\"")]))


(defn generate-prompt-msg
  "Generates a prompt message, including shell color-coding, asking if the commit should continue against the 'branch'."
  [branch]
  (common/apply-display-with-shell
   (str "\"" common/shell-color-white "Type 'yes' if you wish to continue the commit to branch '" branch "'.  Any other input aborts the commit." common/shell-color-reset "\"")))


(defn generate-prompt
  "Generates a prompt, including shell color-coding, that does not include a newline."
  []
  (common/apply-display-with-shell-without-newline (common/apply-quotes ">> ")))


(defn generate-proceed-msg
  "Generates a proceed message, including shell color-coding."
  [branch]
  (common/apply-display-with-shell
   (str "\"" common/shell-color-red "Proceeding with the commit to branch '" branch "'." common/shell-color-reset "\"")))


(defn generate-abort-msg
  "Generates an abort message, including shell color-coding."
  [branch]
  (common/apply-display-with-shell
   (str "\"" common/shell-color-red "Aborting the commit to branch '" branch "'." common/shell-color-reset "\"")))


(defn proceed
  "Prints a message that the commit will proceed and exits with code 0."
  [branch]
  (common/run-shell-command (generate-proceed-msg branch))
  (common/exit-now! 0))


(defn abort
  "Prints an abort message and cancels the commit using exit code 1."
  [branch]
  (common/run-shell-command (generate-abort-msg branch))
  (common/exit-now! 1))


;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-warn
  "Displays a warning about a commit to a protected branch and waits for the user to confirm."
  [branches]
  (let [branch (common/get-git-branch)]
    (when (some #(= branch %) branches)
      (common/run-shell-command (generate-warn-msg branch))
      (common/run-shell-command (generate-prompt-msg branch))
      (common/run-shell-command (generate-prompt)) 
      (let [tty (io/reader (io/file "/dev/tty"))]
        (binding [*in* tty]
          (let [resp (str/trim (read-line))]
            (if (= resp "yes")
              (proceed branch)
              (abort branch))))))))


(defn ^:impure -main
  "Displays a warning about a commit to a protected branch and waits for the user to confirm."
  [& args]
  (perform-warn protected-branches))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

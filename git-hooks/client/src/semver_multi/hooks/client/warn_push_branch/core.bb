#!/usr/bin/env bb

;; (c) Copyright 2024-2025 KineticFire. All rights reserved.
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


;; client-side 'pre-push' git hook to warn on pushing to a protected branch
;;   - parameters
;;      - name of remote to which push is being done [not used]
;;      - location (url) to whichi push is being done [not used]
;;   -  stdin
;;      - Information about the commits which are being pushed is supplied as
;;        lines in this format:
;;         - <local ref> <local oid> <remote ref> <remote oid>


(ns semver-multi.hooks.client.warn-push-branch.core
  (:require [clojure.string             :as str]
            [clojure.java.io            :as io]
            [semver-multi.common.string :as cstr]
            [semver-multi.common.shell  :as cshell]
            [semver-multi.common.system :as system]))



;; version updated by CI pipeline
(def ^:const version "latest")

(def ^:const protected-branches ["main"])


(defn generate-warn-msg
  "Generates a warning message, including shell color-coding, about an attempt push to 'branches'."
  [branches]
  (cshell/apply-display-with-shell
   [(str "\"" cshell/shell-color-red "WARNING\"")
    (str "\"" cshell/shell-color-red "You are attempting to push to branches [" (str/join ", " branches) "]." cshell/shell-color-reset "\"")]))


(defn generate-prompt-msg
  "Generates a prompt message, including shell color-coding, asking if the push should continue against the 'branches'."
  [branches]
  (cshell/apply-display-with-shell
   (str "\"" cshell/shell-color-white "Type 'yes' if you wish to continue the push to branches [" (str/join ", " branches) "].  Any other input aborts the push." cshell/shell-color-reset "\"")))


(defn generate-prompt
  "Generates a prompt, including shell color-coding, that does not include a newline."
  []
  (cshell/apply-display-with-shell-without-newline (cshell/apply-quotes ">> ")))


(defn generate-proceed-msg
  "Generates a proceed message, including shell color-coding."
  [branches]
  (cshell/apply-display-with-shell
   (str "\"" cshell/shell-color-red "Proceeding with the push to branches [" (str/join ", " branches) "]." cshell/shell-color-reset "\"")))


(defn generate-abort-msg
  "Generates an abort message, including shell color-coding."
  [branches]
  (cshell/apply-display-with-shell
   (str "\"" cshell/shell-color-red "Aborting the push to branches [" (str/join ", " branches) "]." cshell/shell-color-reset "\"")))


(defn proceed
  "Prints a message that the push will proceed and exits with code 0."
  [branch]
  (cshell/run-shell-command (generate-proceed-msg branch))
  (system/exit-now! 0))


(defn abort
  "Prints an abort message and cancels the push using exit code 1."
  [branch]
  (cshell/run-shell-command (generate-abort-msg branch))
  (system/exit-now! 1))


(defn get-affected-branches
  "Returns the affected branches in a vector."
  [lines]
  (if (empty? lines)
    []
    (into [] (distinct (map #(last (str/split (nth (str/split % #" ") 2) #"/")) (cstr/split-lines (str/trim lines)))))))


(defn get-affected-protected-branches
  "Returns a vector of the protected branches affected by the push."
  [lines branches]
  (into [] (remove nil? (map #(when (.contains branches %) %) (get-affected-branches lines)))))


;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-warn
  "Displays a warning about a push to a protected branch and waits for the user to confirm."
  [branches]
  (let [affected-protected-branches (get-affected-protected-branches (slurp *in*) branches)]
    (when (some? affected-protected-branches)
      (cshell/run-shell-command (generate-warn-msg affected-protected-branches))
      (cshell/run-shell-command (generate-prompt-msg affected-protected-branches))
      (cshell/run-shell-command (generate-prompt))
      (let [tty (io/reader (io/file "/dev/tty"))]
        (binding [*in* tty]
          (let [resp (str/trim (read-line))]
            (if (= resp "yes")
              (proceed affected-protected-branches)
              (abort affected-protected-branches))))))))


(defn ^:impure -main
  "Displays a warning about a push to a protected branch and waits for the user to confirm."
  [& args]
  (perform-warn protected-branches))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

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


;; client-side 'pre-push' git hook to warn on pushing to a protected branch
;;   - parameters
;;      - name of remote to which push is being done [not used]
;;      - location (url) to whichi push is being done [not used]
;;   -  stdin
;;      - Information about the commits which are being pushed is supplied as
;;        lines in this format:
;;         - <local ref> <local oid> <remote ref> <remote oid>


(ns client-side-hooks.warn-push-branch.core
  (:require [clojure.string    :as str]
            [clojure.java.io   :as io]
            [babashka.process  :refer [shell]]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")

(def ^:const protected-branches ["main"])


(defn generate-warn-msg
  "Generates a warning message, including shell color-coding, about an attempt push to 'branch'."
  [branch]
  (common/apply-display-with-shell
   [(str "\"" common/shell-color-red "WARNING\"")
    (str "\"" common/shell-color-red "You are attempting to push to branch '" branch "'." common/shell-color-reset "\"")]))


(defn generate-prompt-msg
  "Generates a prompt message, including shell color-coding, asking if the push should continue against the 'branch'."
  [branch]
  (common/apply-display-with-shell
   (str "\"" common/shell-color-white "Type 'yes' if you wish to continue the push to branch '" branch "'.  Any other input aborts the push." common/shell-color-reset "\"")))


(defn generate-prompt
  "Generates a prompt, including shell color-coding, that does not include a newline."
  []
  (common/apply-display-with-shell-without-newline (common/apply-quotes ">> ")))


(defn generate-proceed-msg
  "Generates a proceed message., including shell color-coding."
  [branch]
  (common/apply-display-with-shell
   (str "\"" common/shell-color-red "Proceeding with push to branch '" branch "'." common/shell-color-reset "\"")))


(defn generate-abort-msg
  "Generates a proceed message., including shell color-coding."
  [branch]
  (common/apply-display-with-shell
   (str "\"" common/shell-color-red "Aborting push to branch '" branch "'." common/shell-color-reset "\"")))


(defn ^:impure get-git-branch
  "Returns the branch name active in the repo or 'nil' if the command was not executed in a git repo or the command
   failed."
  []
  (let [resp (-> (shell {:out :string :err :string :continue true} "git rev-parse --abbrev-ref HEAD")
                 (select-keys [:out :err]))]
    (if (empty? (:out resp))
      nil
      (str/trim (:out resp)))))


(defn proceed
  [branch]
  (common/run-shell-command (generate-proceed-msg branch))
  (common/exit-now! 1)) ;; todo change back to 0


(defn abort
  [branch]
  (common/run-shell-command (generate-abort-msg branch))
  (common/exit-now! 1))


(defn get-affected-branches
  []
  )

(defn get-affected-protected-branches
  [lines]
  (println "stdin-----")
  (println lines)
  (println "-----stdin"))

;; todo
;; sample output:
    ;;   - refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc
    ;;   - refs/heads/1-test 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/1-test 9591a6519b30e816aaeadec8e30f8731906bd707


;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-warn
  "Displays a warning about a push to a protected branch and waits for the user to confirm."
  [branches]
  (let [branch "none"
        branches ["a" "b"]]
    
    (get-affected-protected-branches (slurp *in*))

    ;; todo for testing
    (System/exit 1)

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
  "Displays a warning about a push to a protected branch and waits for the user to confirm."
  [& args]
  (perform-warn protected-branches))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

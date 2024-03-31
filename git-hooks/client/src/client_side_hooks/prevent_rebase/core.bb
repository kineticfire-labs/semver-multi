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

;; a 'pre-rebase' git hook
;;   - parameters
;;      - upstream from which series was forked
;;      - the branch being rebased; not set when rebasing current branch


(ns client-side-hooks.prevent-rebase.core
  (:require [clojure.string    :as str]
            [babashka.process  :refer [shell]]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")


(defn generate-err-msg
  []
  (common/apply-display-with-shell
   [(str "\"" common/shell-color-red "REBASE REJECTED\"")
    (str "\"" common/shell-color-red "Reason: rebase not allowed becase it destroys commit history" common/shell-color-reset "\"")]))


;; Moved functionality from 'main' to this function for testability due to the const 'default-config-file'
(defn ^:impure perform-prevent-rebase
  "Prevents rebase.  Displays an error message using shell color coding and returns exit code 1 to prevent the rebase."
  []
  (common/run-shell-command (generate-err-msg))
  (common/exit-now! 1))


(defn ^:impure -main
  "Prevents rebase.  Displays an error message using shell color coding and returns exit code 1 to prevent the rebase."
  [& args]
  (perform-prevent-rebase))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

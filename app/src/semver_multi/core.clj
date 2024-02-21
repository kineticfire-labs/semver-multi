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



(ns semver-multi.core
  (:gen-class))



(def ^:const default-config-file "project.def.json")



(defn ^:impure handle-ok
  "Exits with exit code 0."
  []
  (System/exit 0))


(defn ^:impure handle-err
  "Displays message string `msg` to standard out and then exits with exit code 1."
  [msg]
  (println msg)
  (System/exit 1))


(defn process-cli-options
  [cli-args default-config-file]
  (let [err-msg-pre "Invalid options format."
        err-msg-post "\n   - Usage w/ repository access : semver-multi --repository <path to git repository> |--project-def-file <path to project def file, if not named 'project.def.json' in the root of the repository>\n   - Usage w/o repository access: semver-multi --tag <last git tag> --tag-msg <last tag message> --commit-log <commit log from last tag (exclusive) to current (inclusive) --project-def-file <path to project def file>"
        num-cli-args (count cli-args)]
    (if (or
         (and
          (> num-cli-args 0)
          (< num-cli-args 8))
         (> num-cli-args 8))
      {:success false
       :reason (str err-msg-pre " Expected 0, 2, or 8 CLI arguments but received " num-cli-args " arguments." err-msg-post)}
      {:success true})))


(defn ^:impure startup
  [cli-args default-config-file]
  (let [options (process-cli-options cli-args default-config-file)]
    (if (:success options)
      (println "ok")
      (handle-err (:reason options)))))


(defn ^:impure -main
  "Starts up semver-multi"
  [& args]
  (startup args default-config-file))

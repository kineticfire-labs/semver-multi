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



(ns util.semver-ver.core
  (:require [clojure.string    :as str]
            [babashka.process  :refer [shell]]
            [clojure.java.io   :as io]
            [cheshire.core     :as json]
            [common.core       :as common]))



;; version updated by CI pipeline
(def ^:const version "latest")

(def ^:const default-config-file "project.def.json")

(def ^:const default-version-file "version.json")

(def ^:const err-msg-mode "Usage: Must set mode as one of '--create', '--validate', or '--tag'.")

(def ^:const err-msg-create ["Usage for 'create': semver-ver --create --version <version> --project-def-file <file>" "'--version' is optional, default to '1.0.0' and '--project-def-file' is optional if in Git repo."])

(def ^:const err-msg-validate ["Usage for 'validate': semver-ver --validate --version-file <file> --project-def-file <file>", "'--project-def-file' is optional if in Git repo."])

(def ^:const err-msg-tag ["Usage for 'tag': semver-ver --tag --tag--name <name> --version-file <file> --no-warn", "'--no-warn' is optional"])



(defn ^:impure handle-ok
  "Exits with exit code 0."
  []
  (common/exit-now! 0))


(defn ^:impure handle-err
  "Displays message string `msg` to standard out and then exits with exit code 1."
  [msg]
  (println msg)
  (common/exit-now! 1))


;; semver-ver --create --version 1.0.0 --project-def-file <file>  => 1, 3, 5
;; ... version optional if want 1.0.0
;; ... project-def-file optional if in git repo

;; --validate --version-file <file> --project-def-file <file>     => 3, 5
;; ... check:
;;        - all scopes represented
;;        - no additional scopes
;;        - sub thing version not greater than parent? Warn?
;;        - "since" not greater than

;; --tag --tag--name <name> --version-file <file> --no-warn       => 5, 6
;; ... no-warn is optional
;; ... check:
;;        - validate version file
;;        - validate tag name
;;        - check last tag in repo?
;; command: git tag -a -F <file>



(defn flag?
  "Returns 'true' if 'value' is a flag and 'false' if 'value' is not a flag."
  [value]
  (if (str/starts-with? "-")
    true
    false))


(defn duplicate-flag?
  "Returns 'true' if 'flag' is defined in 'defined' and 'false' otherwise."
  [flag defined]
  (if (.contains flag defined)
    true
    false))

(defn mode-defined?
  "Returns 'true' if the mode is defined and 'false' otherwise.  The mode is
   either:  :create, :validate, :tag."
  [defined]
  (if (or
       (.contains :create defined)
       (.contains :validate defined)
       (.contains :tag defined))
    true
    false))


(defn handle-mode
  [defined]
  (if (mode-defined? defined)
    {:success false
     :reason (str "Duplicate mode defined." err-msg-mode)}
    {:success true})) ;; todo... should update response?


(defn get-next-non-flag
  [response defined args flag err-msg]
  (let [arg (first (rest args))
        args (rest (rest args))]
    (if (nil? arg)
      {:success false
       :reason err-msg}
      (if (flag? arg)
        {:success false
         :reason err-msg}
        {:success true
         :response (assoc response flag arg)
         :defined (conj defined flag)
         :args args}))))


(defn process-options-mode-create
  [response defined args]
  ;; handle-mode
  ;; get-next-non-flag
  )


(defn process-options-mode-validate
  [response defined args])


(defn process-options-mode-tag
  [response defined args])


(defn process-options-version
  [response defined args])


(defn process-options-project-def-file
  [response defined args])


(defn process-options-version-file
  [response defined args])


(defn process-options-tag-name
  [response defined args])


(defn process-options-no-warn
  [response defined args])


(defn process-options-no-flag
  [response defined args])


(defn check-response
  "Checks that the CLI arguments were processed correctly and adjust the 'response', if necessary."
  [response]
  ;; todo check response before emitting it
  response)


(defn process-cli-options
  [cli-args config-file version-file]
  (let [err-msg-pre "Invalid options format."
        num-cli-args (count cli-args)]
    (if (not (or
              (= num-cli-args 1)
              (= num-cli-args 3)
              (= num-cli-args 5)
              (= num-cli-args 6)))
      {:success false
       :reason (str err-msg-pre " Expected 1, 3, 5, or 6 CLI arguments but received " num-cli-args " arguments." err-msg-mode)}
      (loop [response {:success true}
             defined []
             args cli-args]
        (if (empty? args)
          (check-response response)
          (let [arg (first args)
                result (case arg
                         "--create"           (process-options-mode-create response defined args)
                         "--validate"         (process-options-mode-validate response defined args)
                         "--tag"              (process-options-mode-tag response defined args)
                         "--version"          (process-options-version response defined args)
                         "--project-def-file" (process-options-project-def-file response defined args)
                         "--version-file"     (process-options-version-file response defined args)
                         "--tag-name"         (process-options-tag-name response defined args)
                         "--no-warn"          (process-options-no-warn response defined args)
                         (process-options-no-flag response defined args))]
            (if (not (:success result))
              (assoc result :reason (str err-msg-pre " " (:reason result) " " " todo: add err msg. or call (check-response)?"))
              (recur (:response result) (:defined result) (:args result)))))))))


;; Implemented 'main' functionality here for testability due to constants
(defn ^:impure perform-main
  ""
  [cli-args config-file version-file]
  (let [options (process-cli-options cli-args config-file version-file)]
    (if (:success options)
      (println "ok")
      (handle-err (:reason options)))))


(defn ^:impure -main
  ""
  [& args]
  (perform-main args default-config-file default-version-file))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

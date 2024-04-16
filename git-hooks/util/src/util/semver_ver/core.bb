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

(def ^:const cli-flags-non-mode
  {"--version"          :version
   "--project-def-file" :project-def-file
   "--version-file"     :version-file
   "--tag-name"         :tag-name
   "--no-warn"          :no-warn})

(def ^:const usage 
  (str
   "Usage: Must set mode as one of '--create', '--validate', or '--tag':\n"
   "   'create': semver-ver --create --version <version> --project-def-file <file>\n"
   "      '--version' is optional, default to '1.0.0' and '--project-def-file' is optional if in Git repo.\n"
   "   'validate': semver-ver --validate --version-file <file> --project-def-file <file>\n"
   "      '--project-def-file' is optional if in Git repo\n"
   "   'tag': semver-ver --tag --tag-name <name> --version-file <file> --no-warn\n"
   "      '--no-warn' is optional"))



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
  (if (str/starts-with? value "-")
    true
    false))


(defn mode-defined?
  "Returns 'true' if the mode is defined and 'false' otherwise.  The mode is
   either:  :create, :validate, :tag."
  [defined]
  (if (or
       (.contains defined :create )
       (.contains defined :validate)
       (.contains defined :tag))
    true
    false))


(defn handle-mode
  [response defined args mode]
  (if (mode-defined? defined)
    {:success false
     :reason "Duplicate mode defined."}
    {:success true
     :response (assoc response :mode mode)
     :defined (conj defined mode)
     :args (rest args)}))


(defn process-options-mode-create
  "Processes the flag '--create'."
  [response defined args]
  (handle-mode response defined args :create))


(defn process-options-mode-validate
  "Processes the flag '--validate'."
  [response defined args]
  (handle-mode response defined args :validate))


(defn process-options-mode-tag
  "Processes the flag '--tag'."
  [response defined args]
  (handle-mode response defined args :tag))


;; --no-warn is handled seprately from the other options since it does not take an argument
(defn process-options-no-warn
  "Processes the flag '--no-warn'."
  [response defined args]
  (if (.contains defined :no-warn)
    {:success false
     :reason (str "Duplicate definition of flag '--no-warn'.")}
    {:success true
     :response (assoc response :no-warn true)
     :defined (conj defined :no-warn)
     :args (rest args)}))


(defn process-options-other
  "Processes options such that an option must be '<flag> <arg>'.  Does not validate flag or arg."
  [response defined args item my-cli-flags-non-mode]
  (if-not (flag? item)
    {:success false
     :reason (str "Expected flag but received non-flag '" item "'.")}
    (let [next-item (first (rest args))
          rest-args (rest (rest args))]
      (if (nil? next-item)
        {:success false
         :reason (str "Expected argument following flag '" item "' but found none.")}
        (if (flag? next-item)
          {:success false
           :reason (str "Expected argument following flag '" item "' but found flag '" next-item "'.")}
          (if-not (contains? my-cli-flags-non-mode item)
            {:success false
             :reason (str "Flag '" item "' not recognized.")}
            (if (.contains defined (get my-cli-flags-non-mode item))
              {:success false
               :reason (str "Duplicate definition of flag '" item "'.")}
              {:success true
               :response (assoc response (get my-cli-flags-non-mode item) next-item)
               :defined (conj defined (get my-cli-flags-non-mode item))
               :args rest-args})))))))


(defn check-response
  "Checks that the CLI arguments were processed correctly and adjust the 'response', if necessary.  Checks syntax of
   command but does NOT validate the flags/arguments.
   
   The 'response' argument is a map such that:  only defined flags set, flags are not dulpicated, flags that expect args
   have them, mode (version, validate, tag) are not dulicated."
  [response]
  ;; todo check response before emitting it
  (println response)
  (println "hi")
  response)


(defn process-cli-options
  [cli-args my-cli-flags-non-mode]
  (let [err-msg-pre "Invalid options format."
        num-cli-args (count cli-args)]
    (if (not (or
              (= num-cli-args 1)
              (= num-cli-args 3)
              (= num-cli-args 5)
              (= num-cli-args 6)))
      {:success false
       :reason (str err-msg-pre " Expected 1, 3, 5, or 6 CLI arguments but received " num-cli-args " arguments.")}
      (loop [response {:success true}
             defined []
             args cli-args]
        (if (empty? args)
          (check-response response)
          (let [arg (str/trim (first args))
                result (case arg
                         "--create"   (process-options-mode-create response defined args)
                         "--validate" (process-options-mode-validate response defined args)
                         "--tag"      (process-options-mode-tag response defined args)
                         "--no-warn"  (process-options-no-warn response defined args)
                         (process-options-other response defined args arg my-cli-flags-non-mode))]
            (if (not (:success result))
              (assoc result :reason (str err-msg-pre " " (:reason result)))
              (recur (:response result) (:defined result) (:args result)))))))))


;; Implemented 'main' functionality here for testability due to constants
(defn ^:impure perform-main
  ""
  [cli-args my-cli-flags-non-mode my-usage config-file version-file]
  (let [options (process-cli-options cli-args my-cli-flags-non-mode)]
    (if (:success options)
      (do
        (println "ok")
        (println options)) ;; todo: apply defaults to cli-options:  config-file and version-file
      (handle-err (str (:reason options) "\n\n" my-usage)))))


(defn ^:impure -main
  ""
  [& args]
  (perform-main args cli-flags-non-mode usage default-config-file default-version-file))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

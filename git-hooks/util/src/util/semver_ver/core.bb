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
  {"--type"             :type
   "--version"          :version
   "--project-def-file" :project-def-file
   "--version-file"     :version-file
   "--no-warn"          :no-warn})

(def ^:const usage 
  (str
   "Usage: Must set mode as one of '--create', '--validate', or '--tag':\n"
   "   'create': semver-ver --create --type <release or update> --version <version> --project-def-file <file> --version-file <file>\n"
   "      '--type' is optional and defaults to 'release', '--version' is optional and defaults to '1.0.0', '--project-def-file' is optional if in Git repo, and '--version-file' is optional and defaults to 'version.json' in current path\n"
   "   'validate': semver-ver --validate --version-file <file> --project-def-file <file>\n"
   "      '--project-def-file' is optional if in Git repo and '--version-file' is optional and defaults to 'version.json' in current path\n"
   "   'tag': semver-ver --tag --version-file <file> --no-warn\n"
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
  "Handles the mode as create, validate, or tag.  Returns a map with key response containing the mode.  Returns an error
   map if the mode is already set."
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


(defn check-response-keys
  "Check the `response` map against the `required` and `optional` vectors, where `required` defines required keys and
   `optionals` defines optional keys. Returns the `response` map if valid else a map with 'success=false' and key reason
   set to the string reason for failure.  
   
   The map `response` must have the mapping 'success=true'."
  [response mode required optional]
  (let [response-pruned (dissoc response :success)
        all-keys (concat required optional)
        extra-keys (remove nil? (map (fn [key] (when-not (.contains all-keys key) key)) (keys response-pruned)))]
    (if-not (empty? extra-keys)
      {:success false
       :reason (str "Mode " mode " doesn't allow keys '" (str/join "," extra-keys) "'.")}
      (let [missing-required-keys (remove nil? (map (fn [key] (when-not (.contains (into [] (keys response-pruned)) key) key)) required))]
        (if-not (empty? missing-required-keys)
          {:success false
           :reason (str "Mode " mode " requires missing keys '" (str/join "," missing-required-keys) "'.")}
          response)))))


(defn check-response-mode-create
  "Check the `response` map for required and optional keys. Returns the `response` map if valid else a map with
     'success=false' and key reason set to the string reason for failure.
     
     The map `response` must have the mapping 'success=true'."
  [response]
  (let [response (check-response-keys response :create [:mode] [:type :version :project-def-file :version-file])]
   (if (:success response)
     (if (some? (:type response))
       (if (or
            (= (:type response) "release")
            (= (:type response) "update"))
         response
         {:success false
          :reason (str "Argument ':type' must be either 'release' or 'update' but was '" (:type response) "'.")})
       response)
     response)))


(defn check-response-mode-validate
  "Check the `response` map for required keys (no optional keys). Returns the `response` map if valid else a map with
     'success=false' and key reason set to the string reason for failure.
     
     The map `response` must have the mapping 'success=true'."
  [response]
  (check-response-keys response :validate [:mode] [:version-file :project-def-file]))


(defn check-response-mode-tag
  "Check the `response` map for required and optional keys. Returns the `response` map if valid else a map with
     'success=false' and key reason set to the string reason for failure.
     
     The map `response` must have the mapping 'success=true'."
  [response]
  (check-response-keys response :tag [:mode :version-file] [:no-warn]))


(defn check-response
  "Checks that the CLI arguments were processed correctly and adjust the 'response', if necessary.  Checks syntax of
   command but does NOT validate the flags/arguments.
   
   The 'response' argument is a map such that:  only defined flags set, flags are not dulpicated, flags that expect args
   have them, mode (version, validate, tag) are not dulicated."
  [response]
  (if-not (:mode response)
    {:success false
     :reason "No mode set. Exactly one mode must be set:  --create, --validate, or --tag."}
    (let [mode (:mode response)]
      (case mode
        :create   (check-response-mode-create response)
        :validate (check-response-mode-validate response)
        :tag      (check-response-mode-tag response)
        {:success false
         :reason (str "Mode '" mode "' not recognized.")}))))


(defn process-cli-options
  "Processes and returns the CLI options set in the sequence `cli-args`.  Validates the `cli-args` and, if valid,
   returns a map with 'success=true' and keys that describe the arguments and the values mapped to their values.  If
   invalid, then returns a map with 'success=false' and 'reason' set to the reason for the failure.  The argument
   `my-cli-non-flags` defines the allowable flags in addition to those that define the mode of create, validate, and
   tag."
  [cli-args my-cli-flags-non-mode]
  (let [err-msg-pre "Invalid options format."
        num-cli-args (count cli-args)]
    (if (not (> num-cli-args 0))
      {:success false
       :reason (str err-msg-pre " Expected 1 or more arguments but received no arguments.")}
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


(defn apply-default-options-mode-create
  "Adds default options if not set for :version, :project-def-file, and :version-file"
  [options git-root-dir default-config-file default-version-file]
  (let [options (if-not (contains? options :type)
                  (assoc options :type "release")
                  options)
        options (if-not (contains? options :version)
                  (assoc options :version "1.0.0")
                  options)
        options (if-not (contains? options :version-file)
                  (assoc options :version-file default-version-file)
                  options)]
    (if-not (contains? options :project-def-file)
      (if-not (nil? git-root-dir)
        (assoc options :project-def-file (str git-root-dir "/" default-config-file))
        {:success false
         :reason "The project-def.json file must be specified with --project-def-file, or this script must be executed from within a Git repository."})
      options)))


(defn apply-default-options-mode-validate
  "Adds default options if not set for :project-def-file and :version-file."
  [options git-root-dir default-config-file default-version-file]
  (let [options (if-not (contains? options :version-file)
                  (assoc options :version-file default-version-file)
                  options)]
    (if-not (contains? options :project-def-file)
      (if-not (nil? git-root-dir)
        (assoc options :project-def-file (str git-root-dir "/" default-config-file))
        {:success false
         :reason "The project-def.json file must be specified with --project-def-file, or this script must be executed from within a Git repository."})
      options)))


(defn apply-default-options-mode-tag
  "Returns `options` un-modified.  There are no default options."
  [options]
  options)


(defn apply-default-options
  "Applies default options."
  [options git-root-dir default-config-file default-version-file]
  (case (:mode options)
    :create   (apply-default-options-mode-create options git-root-dir default-config-file default-version-file)
    :validate (apply-default-options-mode-validate options git-root-dir default-config-file default-version-file)
    :tag      (apply-default-options-mode-tag options)))


;; Implemented 'main' functionality here for testability due to constants
(defn ^:impure perform-main
  ""
  [params]
  (let [options (process-cli-options (:cli-args params) (:cli-flags-non-mode params))]
    (if (:success options)
      (let [options (apply-default-options options (:git-root-dir params) (:default-config-file params) (:default-version-file params))]
        (if (:success options)
          (let [options (dissoc options :success)]
            (println "ok" options))
          (handle-err (str (:reason options) "\n\n" (:usage params)))))
      (handle-err (str (:reason options) "\n\n" (:usage params))))))


(defn ^:impure -main
  ""
  [& args]
  (perform-main {
                 :cli-args             args
                 :cli-flags-non-mode   cli-flags-non-mode
                 :usage                usage
                 :git-root-dir         (common/get-git-root-dir)
                 :default-config-file  default-config-file
                 :default-version-file default-version-file
                 }))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

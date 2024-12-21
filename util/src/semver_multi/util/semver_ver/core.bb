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


;; KineticFire Labs: https://labs.kineticfire.com/
;;	   Project site: https://github.com/kineticfire-labs/semver-multi/



(ns semver-multi.util.semver-ver.core
  (:require [clojure.string                  :as str]
            [cheshire.core                   :as json]
            [semver-multi.common.system      :as system]
            [semver-multi.common.version     :as version]
            [semver-multi.common.file        :as file]
            [semver-multi.common.project-def :as proj]
            [semver-multi.common.git         :as git]
            [semver-multi.common.util        :as cutil]))



;; version updated by CI pipeline
(def ^:const version "latest")

(def ^:const default-remote-name "origin")

(def ^:const default-version-file "version.dat")

;; todo: changed format, see README
(def ^:const default-update-data
  {:type "update"
   :add [{:example.alpha "1.0.0"}]
   :remove [:example.bravo]
   :move {:example.from.charlie :example.to.echo.charlie}})

(def ^:const cli-flags-non-mode
  {"--type"                      :type
   "--version"                   :version
   "--version-file"              :version-file
   "--remote-name"               :remote-name})

(def ^:const usage 
  (str
   "This utility is typically used to:\n"
   "   (1) create one-time initialization version data for a project, using the '--create' flag\n"
   "   (2) update project/artifact structure and contents, using the '--update' flag\n"
   "   (3) validate initial version data (from #1) or project/artifact structure updates (from #2), using the\n"
   "       '--validate' flag\n"
   "   (4) tag initial version data (from #1) or project/artifact structure updates (from #2), using the '--tag' flag\n"
   "\n"
   "Outside of the cases above, version data shouldn't need to be manually generated and should be created by\n"
   "semver-multi as part of the CI/CD process.\n"
   "\n" 
   "USAGE: Must be executed from the root of the Git repo on the desired branch in the desired state, which must contain\n"
   " a valid project definition file named 'semver-multi.json' at the project root, and must set the mode as one of\n"
   "'--create', '--validate', or '--tag':\n"
   "\n"
   "   'create': Creates version data.\n"
   "      USAGE:\n"
   "         semver-ver --create --type <release, test-release, or update> --version <version> --version-file <file>\n"
   "      DESCRIPTION:\n"
   "         '--type' defines version type to create as either 'release' (e.g., create initial release version data) or\n"
   "            'update (e.g., define project/artifact structure updates)'; optional, defaults to 'release'\n"
   "         '--version' defines the semantic version to populate for all scopes; optional, defaults to '1.0.0'\n"
   "         '--version-file' specifies the version data file to validate; optoinal, defaults to 'version.dat' in the\n"
   "            current working directory\n"
   "\n"
   "   'validate': Validates version data.\n"
   "      USAGE:\n"
   "         semver-ver --validate --version-file <file>\n"
   "      DESCRIPTION:\n"
   "         '--version-file' specifies the version data file to validate; optoinal, defaults to 'version.dat' in the\n"
   "            current working directory\n"
   "\n"
   "   'tag': Produces a Git annotated tag with version data.  Version data is validated prior to tagging, and the tag\n"
   "      is pushed to the remote server.\n"
   "      USAGE:\n"
   "         semver-ver --tag --version-file <file> --remote-name <name>\n"
   "      DESCRIPTION:\n"
   "         '--version-file' specifies the version data file to use\n"
   "         '--remote-name' specifies the Git remote name; optional, defaults to 'origin'\n"))



(defn ^:impure handle-ok
  "Exits with exit code 0."
  []
  (system/exit-now! 0))


(defn ^:impure handle-err
  "Displays message string `msg` to standard out and then exits with exit code 1."
  [msg]
  (println msg)
  (system/exit-now! 1))


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


(defn is-create-type?
  "Returns boolean 'true' if `type` is a valid type for the :create mode and 'false' otherwise."
  [type]
  (let [valid-types ["release" "test-release" "update"]]
    (if (.contains valid-types type)
      true
      false)))


(defn is-optional-create-type?
  "Returns boolean 'true' if `type` is a valid type for the :create mode or if `type` is nil, else returns 'false'."
  [type]
  (if (some? type)
    (is-create-type? type)
    true))

(defn is-optional-semantic-version-release?
  "Returns 'true' if `version` is a valid semantic version for a release or if `version` is nil, else returns 'false'."
  [version]
  (if (some? version)
    (cutil/is-semantic-version-release? version)
    true))


(defn check-response-mode-create
  "Check the `response` map for required and optional keys. Returns the `response` map if valid else a map with
     'success=false' and key reason set to the string reason for failure.
     
     The map `response` must have the mapping 'success=true'."
  [response]
  (let [response (check-response-keys response :create [:mode] [:type :version :version-file])]
   (if (:success response)
     (if (is-optional-create-type? (:type response))
       (if (is-optional-semantic-version-release? (:version response))
         response
         {:success false
          :reason (str "Argument ':version' must be a valid semantic version release number but was '" (:version response) "'.")})
       {:success false
        :reason (str "Argument ':type' must be either 'release', 'test-release', or 'update' but was '" (:type response) "'.")})
     response)))


(defn check-response-mode-validate
  "Check the `response` map for required keys (no optional keys). Returns the `response` map if valid else a map with
     'success=false' and key reason set to the string reason for failure.
     
     The map `response` must have the mapping 'success=true'."
  [response]
  (check-response-keys response :validate [:mode] [:version-file]))


(defn check-response-mode-tag
  "Check the `response` map for required and optional keys. Returns the `response` map if valid else a map with
     'success=false' and key reason set to the string reason for failure.
     
     The map `response` must have the mapping 'success=true'."
  [response]
  (check-response-keys response :tag [:mode :version-file] [:remote-name]))


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
                         (process-options-other response defined args arg my-cli-flags-non-mode))]
            (if (not (:success result))
              (assoc result :reason (str err-msg-pre " " (:reason result)))
              (recur (:response result) (:defined result) (:args result)))))))))


(defn apply-default-options-mode-create
  "Adds default options if not set for :release, :version, and :version-file.  Sets ':project-def-file'."
  [options git-root-dir default-project-def-file default-version-file]
  (let [options (if-not (contains? options :type)
                  (assoc options :type :release)
                  (assoc options :type (keyword (:type options))))
        options (if-not (contains? options :version)
                  (assoc options :version "1.0.0")
                  options)
        options (if-not (contains? options :version-file)
                  (assoc options :version-file default-version-file)
                  options)
        options (assoc options :project-def-file (str git-root-dir "/" default-project-def-file))]
    options))


(defn apply-default-options-mode-validate
  "Adds default options if not set for ':version-file'.  Sets ':project-def-file'."
  [options git-root-dir default-project-def-file default-version-file]
  (let [options (if-not (contains? options :version-file)
                  (assoc options :version-file default-version-file)
                  options)
        options (assoc options :project-def-file (str git-root-dir "/" default-project-def-file))]
    options))


(defn apply-default-options-mode-tag
  "Adds default options if not set for ':remote-name'.  Sets ':project-def-file'."
  [options git-root-dir default-project-def-file default-remote-name]
  (let [options (if-not (contains? options :remote-name)
                  (assoc options :remote-name default-remote-name)
                  options)
        options (assoc options :project-def-file (str git-root-dir "/" default-project-def-file))]
    options))


(defn apply-default-options
  "Applies default options."
  [options git-root-dir default-project-def-file default-version-file default-remote-name]
  (case (:mode options)
    :create   (apply-default-options-mode-create options git-root-dir default-project-def-file default-version-file)
    :validate (apply-default-options-mode-validate options git-root-dir default-project-def-file default-version-file)
    :tag      (apply-default-options-mode-tag options git-root-dir default-project-def-file default-remote-name)))



(defn create-release-version-data
  "Computes and returns a map representing the version data based on options in `options` and the project definition in
   `project-def-json`.  Both `options` and `project-def-json` must be validated."
  [options project-def-json]
  (let [scopes (proj/scope-list-to-string (proj/get-all-full-scopes project-def-json))
        version-map (apply hash-map (apply concat (map (fn [itm] [(keyword itm) {:version (:version options)}]) scopes)))
        version-data {:type (version/version-type-keyword-to-string (:type options))
                      :project-root (first scopes)
                      :versions version-map}]
    version-data))


(defn ^:impure perform-mode-create-release
  [options project-def-json]
  (let [content (str version/version-data-marker-start "\n"
                     (json/generate-string (create-release-version-data options project-def-json) {:pretty true}) "\n"
                     version/version-data-marker-end "\n")]
    (file/write-file (:version-file options) content)))


(defn ^:impure perform-mode-create-update
  [options]
  (let [content (str version/version-data-marker-start "\n"
                     (json/generate-string default-update-data {:pretty true}) "\n"
                     version/version-data-marker-end "\n")]
    (file/write-file (:version-file options) content)))


;; todo: test-release
(defn ^:impure perform-mode-create
  "Performs the mode ':create' functionality to create and write version data to a file named by ':version-file' in
   `options`, returning a map result with :success true if successful and false otherwise; if 'false', then includes
   ':reason' as the reason for the failure.  The `options` and `project-def-json` must be valid."
  [options project-def-json]
  (case (:type options)
    :release (perform-mode-create-release options project-def-json)
    :update (perform-mode-create-update options)))


;; todo: for perform tag, see notes in "usage" at top
;; todo note for later: `git-branch` is for 'validate' and 'tag'
;; todo: test
(defn ^:impure perform-mode
  "Performs the functionality according to mode of ':create', ':validate', ':tag' set in ':mode' in `options` and
   returns a map result with ':success' true if successful else false.  Argument `input-file-data` must contain key
   ':project-def-json' which holds the JSON parsed configuration file and, if ':mode' is ':validate' or ':tag', key
   ':version-content' which hold the version data content; any input files must be validated."
  [options input-file-data git-branch]
  (case (:mode options)
    :create (perform-mode-create options (:project-def-json input-file-data))))


;; todo finish & test
(defn validate-version-json-if-present
  [version-json]
  (if (nil? version-json)
    {:success true}
    {:success false
     :reason "todo: return to this"}))


;; Implemented 'main' functionality here for testability due to constants
(defn ^:impure perform-main
  "todo"
  [params]
  (if (some? (:git-root-dir params))
    (let [options (process-cli-options (:cli-args params) (:cli-flags-non-mode params))]
      (if (:success options)
        (let [options (apply-default-options options (:git-root-dir params) (:default-project-def-file params) (:default-version-file params) (:default-remote-name params))]
          (if (:success options)
            (let [options (dissoc options :success)
                  input-file-data-result (if (= (:mode options) :create)
                                           (file/get-input-file-data (dissoc options :version-file))
                                           (file/get-input-file-data options))]
              (if (:success input-file-data-result)
                (let [input-file-data-result (dissoc input-file-data-result :success)
                      validate-config-result (proj/validate-config (:project-def-json input-file-data-result))]
                  (if (:success validate-config-result)
                    (let [validate-version-result (validate-version-json-if-present (:version-json input-file-data-result))]
                      (if (:success validate-version-result)
                        (let [result (perform-mode options input-file-data-result (:git-branch params))]
                          (if (:success result)
                            (println "ok!")
                            (handle-err (:reason result))))
                        (handle-err (:reason validate-version-result))))
                    (handle-err (:reason validate-config-result))))
                (handle-err (:reason input-file-data-result))))
            (handle-err (str (:reason options) "\n\n" (:usage params)))))
        (handle-err (str (:reason options) "\n\n" (:usage params)))))
    (handle-err (str "semver-ver must be executed from within a Git repository." "\n\n" (:usage params)))))


;; Notes on control flow:
;;
;; perform-main
;;    process-cli-options
;;       process-options-other
;;       check-response
;;          check-response-mode-{create, validate, tag}
;;    apply-default-options
;;       apply-default-options-mode-{create, validate, tag}
;;    common/get-input-file-data
;;    common/validate-config
;;    common/validate-version-json-if-present (todo)
;;    perform-mode
;;       perform-mode-{create {release, test-release (todo), update}, validate (todo), tag (todo)}

(defn ^:impure -main
  ""
  [& args]
  (perform-main {:cli-args                  args
                 :cli-flags-non-mode        cli-flags-non-mode
                 :usage                     usage
                 :git-root-dir              (git/get-git-root-dir)
                 :git-branch                (git/get-git-branch)
                 :default-project-def-file  proj/default-project-def-file
                 :default-version-file      default-version-file
                 :default-remote-name       default-remote-name}))


;; execute 'main' function if run as a script, but don't execute 'main' if just loading the script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

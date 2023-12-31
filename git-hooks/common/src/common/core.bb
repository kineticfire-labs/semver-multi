#!/usr/bin/env bb

;; (c) Copyright 2023 KineticFire. All rights reserved.
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
;;	  Project site:  https://github.com/kineticfire-labs/git-conventional-commits-hooks

(ns common.core
  (:require [clojure.string    :as str]
            [babashka.cli      :as cli]
            [babashka.process  :refer [shell process check]]
            [clojure.java.io   :as io]
            [cheshire.core     :as json])
  (:import (java.util.regex Pattern)))



(def ^:const shell-color-red "\\e[1m\\e[31m")

(def ^:const shell-color-yellow "\\e[1m\\e[33m")

(def ^:const shell-color-blue "\\e[34m")

(def ^:const shell-color-white "\\e[0m\\e[1m")

(def ^:const shell-color-reset "\\033[0m\\e[0m")


(defn do-on-success
  "Perfroms the function 'fn' if the last argument is a map with key 'success' set to 'true', otherwise returns the last argument."
  ([fn arg]
   (if (:success arg)
     (fn arg)
     arg))
  ([fn arg1 arg2]
   (if (:success arg2)
     (fn arg1 arg2)
     arg2))
  ([fn arg1 arg2 arg3]
   (if (:success arg3)
     (fn arg1 arg2 arg3)
     arg3))
  ([fn arg1 arg2 arg3 arg4]
   (if (:success arg4)
     (fn arg1 arg2 arg3 arg4)
     arg4)))


(defn ^:impure exit
  [value]
  (System/exit value))


(defn split-lines
  "Splits the string 'data' based on an optional carriage return '\r' and newline '\n' and returns the result as a vector.  Same as split-lines, but returns all newlines (including those that are newline-only)."
  [data]
  (clojure.string/split data #"\r?\n" -1))


(defn ^:impure run-shell-command
  "Runs commands in 'lines', as either a string or vector of strings, by using 'shell'."
  [lines]
  (if (= (.getSimpleName (type lines)) "String")
    (run-shell-command [lines])
    (dorun (map shell lines))))


(defn apply-display-with-shell
  "Applies 'echo -e' to each line in 'lines', which supports display to the terminal with color coding, and returns the result.  If argument 'lines' is a string, then returns a string; if 'lines' is a collection of strings, then returns a lazy sequence of strings."
  [lines]
  (if (= (.getSimpleName (type lines)) "String")
    (str "echo -e " lines)
    (map #(str "echo -e " %) lines)))


(defn generate-shell-newline-characters
  "Generates newline characters understood by the terminal and returns the string result.  Displays one newline without arguments or int 'num' newlines."
  ([]
   (generate-shell-newline-characters 1))
  ([num]
   (str/join "" (repeat num "\n"))))


(defn generate-commit-msg-offending-line-header
  "Generates a header that indicates an offending line that was in error, if sequence 'lines-num' is non-nil and non-empty; indexes in 'lines-num' are indexed starting at 0.  Appends the header line to the vector of strings 'lines' and returns the result or, if no header should be generated, returns 'lines' unchanged."
  [lines lines-num]
  (if (empty? lines-num)
    lines
    (conj lines (str "\"   offending line(s) # " (pr-str (map inc lines-num)) " in red **************\""))))


(defn generate-commit-msg-offending-line-msg-highlight
  "Adds shell color-code formatting for an offending line(s) identified in the 'lines-num' sequence to the vector of strings 'lines'.  Contents of 'lines-num' are integer indicies indexed starting at 0.  If 'lines-num' is 'nil' or empty, then 'lines' is returned unchanged."
  [lines lines-num]
  (if (empty? lines-num)
    lines
    (vec (map-indexed (fn [idx line] (if (some (fn [num] (= idx num)) lines-num)
                                       (str shell-color-red line shell-color-reset)
                                       line)) lines))))

(defn generate-commit-msg
  "Generates a formatted commit message, using string 'msg', with optional call-out to the offending line(s) if the 'lines-num' sequence is non-nil and non-empty; 'lines-num' is indexed starting at 0.  Returns the result as a lazy sequence of strings, formatted for shell output with color-coding."
  ([msg]
   (generate-commit-msg msg nil))
  ([msg lines-num]
   (let [msg-vec (split-lines msg)
         start-lines-top
         [(str "\"" shell-color-blue "**********************************************\"")
          "\"BEGIN - COMMIT MESSAGE ***********************\""]
         start-line-end
         (str "\"**********************************************" shell-color-reset "\"")
         end-lines
         [(str "\"" shell-color-blue "**********************************************\"")
          "\"END - COMMIT MESSAGE *************************\""
          (str "\"**********************************************" shell-color-reset "\"")]]
     (apply-display-with-shell
      (into (into (conj (generate-commit-msg-offending-line-header start-lines-top lines-num) start-line-end) (generate-commit-msg-offending-line-msg-highlight msg-vec lines-num)) end-lines)))))


(defn generate-commit-err-msg
  "Generates and returns as a vector of strings an error message including the string 'title' as part of the title and the string 'err-msg' as the reason, formatting the string for shell output with color-coding."
  [title err-msg]
  (apply-display-with-shell
   [(str "\"" shell-color-red "COMMIT REJECTED " title"\"")
    (str "\"" shell-color-red "Commit failed reason: " err-msg shell-color-reset "\"")]))


(defn ^:impure handle-err-exit
  "Generates and displays to the shell an error message, including the string 'title' as part of the title and the string 'err-msg' as the reason, using color-coding from the shell.  Optionally accepts a string 'commit-msg' to display; and optionally accepts a sequence 'line-num' of integer line numbers, indexed at 0, which displays a message about the offending line and highlights it in the commit message or can be 'nil'.  Exits with return code 1."
  ([title err-msg]
   (run-shell-command (generate-commit-err-msg title err-msg))
   (exit 1))
  ([title err-msg commit-msg]
   (run-shell-command (generate-commit-err-msg title err-msg))
   (run-shell-command (generate-commit-msg commit-msg))
   (exit 1))
  ([title err-msg commit-msg line-num]
   (run-shell-command (generate-commit-err-msg title err-msg))
   (run-shell-command (generate-commit-msg commit-msg line-num))
   (exit 1)))


(defn generate-commit-warn-msg
  "Generates and returns as a string a warning message including the string 'title' as part of the title and 'warn-msg' as the reason, formatting the string for shell output with color-coding."
  [title warn-msg]
  (apply-display-with-shell 
   [(str "\"" shell-color-yellow "COMMIT WARNING " title "\"")
    (str "\"" shell-color-yellow "Commit proceeding with warning: " warn-msg shell-color-reset "\"")]))


(defn ^:impure handle-warn-proceed
  "Generates and displays to the terminal a warning message, including the string 'title' as part of the title and 'warn-msg' as the reason, using color-coding from the shell."
  [title warn-msg]
  (run-shell-command (generate-commit-warn-msg title warn-msg)))


(defn ^:impure parse-json-file
  "Reads and parses the JSON config file, 'filename', and returns a map result.  If successful, ':success' is 'true' and 'result' contains the JSON config as a map.  Else ':success' is 'false' and ':reason' describes the failure."
  [filename]
  (let [response {:success false}
        result (try
                 (json/parse-stream-strict (clojure.java.io/reader filename) true)
                 (catch java.io.FileNotFoundException e
                   (str "File '" filename "' not found. " (.getMessage e)))
                 (catch java.io.IOException e
                   ;; Babashka can't find com.fasterxml.jackson.core.JsonParseException, which is thrown for a JSON parse exception.                   
                   ;;   To differentiate the JsonParseException from a java.io.IOException, attempt to 'getMessage' on the exception.
                   (try
                     (.getMessage e)
                     (str "IO exception when reading file '" filename "', but the file was found. " (.getMessage e))
                     (catch clojure.lang.ExceptionInfo ei
                       (str "JSON parse error when reading file '" filename "'.")))))]
    (if (= (compare (str (type result)) "class clojure.lang.PersistentArrayMap") 0)
      (assoc (assoc response :result result) :success true)
      (assoc response :reason result))))


(defn validate-config-fail
  "Returns a map with key ':success' with value boolean 'false' and ':reason' set to string 'msg'.  If map 'data' is given, then associates the map values into 'data'."
  ([msg]
   {:success false :reason msg})
  ([msg data]
   (-> data
       (assoc :success false)
       (assoc :reason msg))))


(defn validate-config-param-string
  "Returns boolean 'true' if the value at vector 'key-path' in map 'data' is a string and 'false' otherwise."
  [data key-path required]
  (if (or required (get-in data key-path))
    (string? (get-in data key-path))
    true))


(defn validate-config-param-array
  "Returns boolean 'true' if for all elements in map 'data' at vector 'key-path' the application of 'fn' to those elements is 'true' and if 'required' is 'true' or if that location is set; 'false' otherwise'."
  [data key-path required fn]
  (if (or required (get-in data key-path))
    (and (vector? (get-in data key-path))
         (> (count (get-in data key-path)) 0)
         (not (.contains (vec (map fn (get-in data key-path))) false)))
    true))


(defn validate-config-msg-enforcement
  "Validates the 'commit-msg-enforcement' fields in the config at key 'config' in map 'data'.  Returns map 'data' with key ':success' set to boolean 'true' if valid or boolean 'false' and ':reason' set to a string message."
  [data]
  (let [enforcement (get-in data [:config :commit-msg-enforcement])
        enabled (:enabled enforcement)]
    (if (some? enforcement)
      (if (nil? enabled)
        (validate-config-fail "Commit message enforcement must be set as enabled or disabled (commit-msg-enforcement.enabled) with either 'true' or 'false'." data)
        (if (boolean? enabled)
          (assoc data :success true)
          (validate-config-fail "Commit message enforcement 'enabled' (commit-msg-enforcement.enabled) must be a boolean 'true' or 'false'." data)))
      (validate-config-fail "Commit message enforcement block (commit-msg-enforcement) must be defined." data))))


(defn validate-config-length
  "Validates the min and max length fields in the config at key 'config' in map 'data'.  Returns map 'data' with key ':success' set to boolean 'true' if valid or boolean 'false' and ':reason' set to a string message."
  [data]
  (let [title-line-min (get-in data [:config :commit-msg :length :title-line :min])
        title-line-max (get-in data [:config :commit-msg :length :title-line :max])
        body-line-min (get-in data [:config :commit-msg :length :body-line :min])
        body-line-max (get-in data [:config :commit-msg :length :body-line :max])]
    (if (some? title-line-min)
      (if (some? title-line-max)
        (if (some? body-line-min)
          (if (some? body-line-max)
            (if (pos-int? title-line-min)
              (if (pos-int? title-line-max)
                (if (>= title-line-max title-line-min)
                  (if (pos-int? body-line-min)
                    (if (pos-int? body-line-max)
                      (if (>= body-line-max body-line-min)
                        data
                        (validate-config-fail "Maximum length of body line (length.body-line.max) must be equal to or greater than minimum length of body line (length.body-line.min)." data))
                      (validate-config-fail "Maximum length of body line (length.body-line.max) must be a positive integer." data))
                    (validate-config-fail "Minimum length of body line (length.body-line.min) must be a positive integer." data))
                  (validate-config-fail "Maximum length of title line (length.title-line.max) must be equal to or greater than minimum length of title line (length.title-line.min)." data))
                (validate-config-fail "Maximum length of title line (length.title-line.max) must be a positive integer." data))
              (validate-config-fail "Minimum length of title line (length.title-line.min) must be a positive integer." data))
            (validate-config-fail "Maximum length of body line (length.body-line.max) must be defined." data))
          (validate-config-fail "Minimum length of body line (length.body-line.min) must be defined." data))
        (validate-config-fail "Maximum length of title line (length.title-line.max) must be defined." data))
      (validate-config-fail "Minimum length of title line (length.title-line.min) must be defined." data))))


(defn validate-config-for-root-project
  "Validates the root project, returning the data with key 'success' to 'true' if valid other 'false' with key 'reason' with the reason.  Root project must be checked for appropriate structure before checking config with recursion.  The root project is different than sub-projects because former structure is a map while latter is a vector."
  [data]
  (let [project (get-in data [:config :project])]
    (if (nil? project)
      (validate-config-fail "Property 'project' must be defined at the top-level." data)
      (if (map? project)
        (assoc data :success true)
        (validate-config-fail "Property 'project' must be a map." data)))))


(defn validate-config-project-artifact-common
  "Validates the project/artifact located at `json-path` in the map `data`, returning the `data` with key 'success' set to 'true' on success and otherwise 'false' with 'reason' reason.  The `node-type` may be either ':project' or ':artifact' so that the error message uses the appropriate descriptor."
  [node-type json-path data]
  (let [node (get-in data json-path)
        node-descr (if (= :project node-type)
                     "Project"
                     "Artifact")]
    (if (validate-config-param-string node [:name] true)
      (let [name (:name node)]
        (if (validate-config-param-string node [:description] false)
          (if (validate-config-param-string node [:scope] true)
            (if (validate-config-param-string node [:scope-alias] false)
              (if (validate-config-param-array node [:types] true string?)
                (if (nil? (:project node))
                  (assoc data :success true)
                  (validate-config-fail (str node-descr " cannot have property 'project' at property 'name' of '" name "' and path '" json-path "'.") data))
                (validate-config-fail (str node-descr " required property 'types' at property 'name' of '" name "' and path '" json-path "' must be an array of strings.") data))
              (validate-config-fail (str node-descr " optional property 'scope-alias' at property 'name' of '" name "' and path '" json-path "' must be a string.") data))
            (validate-config-fail (str node-descr " required property 'scope' at property 'name' of '" name "' and path '" json-path "' must be a string.") data))
          (validate-config-fail (str node-descr " optional property 'description' at property 'name' of '" name "' and path '" json-path "' must be a string.") data)))
      (validate-config-fail (str node-descr " required property 'name' at path '" json-path "' must be a string.") data))))


(defn validate-config-project-specific
  "Validates the project located at `json-path` in the map `data` for project-specific properties, returning the `data` with key 'success' set to 'true' on success and otherwise 'false' with 'reason' reason.  The 'name' in the target `json-path` path in `data` must be validated.  Does NOT validate the individual artifacts, if any."
  [json-path data]
  (let [node (get-in data json-path)
        name (:name node)]
    (if (validate-config-param-array node [:projects] false map?)
      (if (validate-config-param-array node [:artifacts] false map?)
        (assoc data :success true)
        (validate-config-fail (str "Project optional property 'artifacts' at property 'name' of '" name "' and path '" json-path "' must be an array of objects.") data))
      (validate-config-fail (str "Project optional property 'projects' at property 'name' of '" name "' and path '" json-path "' must be an array of objects.") data))))


(defn validate-config-artifact-specific
  "Validates the artifact located at `json-path` in the map `data` for artifact-specific properties, returning the `data` with key 'success' set to 'true' on success and otherwise 'false' with 'reason' reason.  The 'name' in the target `json-path` path in `data` must be validated."
  [json-path data]
  (let [node (get-in data json-path)
        name (:name node)]
    (if (nil? (:projects node))
      (if (nil? (:artifacts node))
        (assoc data :success true)
        (validate-config-fail (str "Artifact cannot have property 'artifacts' at property 'name' of '" name "' and path '" json-path "'.") data))
      (validate-config-fail (str "Artifact cannot have property 'projects' at property 'name' of '" name "' and path '" json-path "'.") data))))


(defn validate-config-artifacts
  "Validates the artifacts, if any defined, located at '`json-path` :artifacts' in the map `data` , returning the `data` with key 'success' set to 'true' on success and otherwise 'false' with 'reason' reason."
  [json-path data]
  (let [json-path-artifacts (conj json-path :artifacts)
        artifacts (get-in data json-path-artifacts)]
    (if (empty? artifacts)
      (assoc data :success true)
      (let [results-common (filter (fn[v] (false? (:success v))) (map-indexed (fn[idx _] (validate-config-project-artifact-common :artifact (conj json-path-artifacts idx) data)) artifacts))]
        (if (empty? results-common)
          (let [results-specific (filter (fn [v] (false? (:success v))) (map-indexed (fn [idx _] (validate-config-artifact-specific (conj json-path-artifacts idx) data)) artifacts))]
            (if (empty? results-specific)
              (assoc data :success true)
              (first results-specific)))
          (first results-common))))))


(defn get-frequency-on-properties-on-array-of-objects
  "Returns a map with the key as the element, found in the `target` sequence with objects for `properties`, and value as the number of occurances of that element if the occurances are two or greater."
  [target properties]
  (filter some? (map (fn [[key value]] (when (>= value 2) key)) (frequencies (apply concat (map (fn [path] (map (fn [project] (get-in project [path])) target)) properties))))))


(defn validate-config-project-artifact-lookahead
  "Validates the array of nodes (projects or artifacts) at the `json-path` in `data` and returns a map with key 'true' if valid and 'false' otherwise with key 'reason'.  Error messages include the `node-type`, set with either ':project', ':artifact', or ':both'.  Returns successful if no nodes found."
  [node-type json-path data]
  (let [nodes (if (coll? (first json-path))
                (into [] (apply concat (map (fn [path] (get-in data path)) json-path)))
                (get-in data json-path))
        node-descr (if (= :project node-type)
                     "Project"
                     (if (= :artifact node-type)
                       "Artifact"
                       "Project/Artifact"))]
    (if (some? nodes)
      (let [name-resp (get-frequency-on-properties-on-array-of-objects nodes [:name])]
        (if (empty? name-resp)
          (let [descr-resp (get-frequency-on-properties-on-array-of-objects nodes [:description])]
            (if (empty? descr-resp)
              (let [scope-resp (get-frequency-on-properties-on-array-of-objects nodes [:scope :scope-alias])]
                (if (empty? scope-resp)
                  (assoc data :success true)
                  (validate-config-fail (str node-descr " has duplicate value '" (apply str scope-resp) "' for required property 'scope' / optional property 'scope-alias' at path '" json-path "'.") data)))
              (validate-config-fail (str node-descr " has duplicate value '" (apply str descr-resp) "' for optional property 'description' at path '" json-path "'.") data)))
          (validate-config-fail (str node-descr " has duplicate value '" (apply str name-resp) "' for required property 'name' at path '" json-path "'.") data)))
      (assoc data :success true))))


;; Uses breadth-first traversal because easier to check for name/scope/alias conflict at same level of tree.  Due to JSOn structure of the config file, the config is acyclic.
(defn validate-config-projects
  "Validates the projects in the config at [:config :project :projects] in `data` returning a map result which is the original `data` with key 'success' to 'true' if valid else set to 'false' with 'reason' set to the reason for the failure.  Does not validate the top-level project."
  [data]
  (loop [queue [[:config :project]]]
    (if (empty? queue)
      (assoc data :success true)
      (let [json-path (first queue)
            result (->> (assoc data :success true)
                        (do-on-success validate-config-project-artifact-common :project json-path)
                        (do-on-success validate-config-project-specific json-path)
                        (do-on-success validate-config-artifacts json-path)
                        (do-on-success validate-config-project-artifact-lookahead :artifact (conj json-path :artifacts))
                        (do-on-success validate-config-project-artifact-lookahead :project (conj json-path :projects))
                        (do-on-success validate-config-project-artifact-lookahead :both [(conj json-path :artifacts) (conj json-path :projects)]))]
        (if (:success result)
          (if (nil? (get-in data (conj json-path :projects)))
            (recur (vec (rest queue)))
            (recur (into (vec (rest queue)) (map (fn [itm] (conj json-path :projects itm)) (range (count (get-in data (conj json-path :projects))))))))
          result)))))


;; Ignores properties not used by this tool to allow other systems to re-use the same project definition
(defn validate-config
  "Performs validation of the config file 'config'.  Returns a map result with key ':success' of 'true' if valid and 'false' otherwise.  If invalid, then returns a key ':reason' with string reason why the validation failed.  Ignores properties not used by this tool to allow other systems to use the same project definition config."
  [config]
  (let [data {:config config :success true}
        result (->> data
                    (do-on-success validate-config-msg-enforcement)
                    (do-on-success validate-config-length)
                    (do-on-success validate-config-for-root-project)
                    (do-on-success validate-config-projects))]
    result))


(defn config-enabled?
  [config]
  (if (:enabled (:commit-msg-enforcement config))
    true
    false))


(defn ^:impure read-file
  "Reads the file 'filename' and returns a map with the result.  Key 'success' is 'true' if successful and 'result' contains the contents of the file as a string, otherwise 'success' is 'false' and 'reason' contains the reason the operation failed."
  [filename]
  (let [response {:success false}
        result (try
                 (slurp filename)
                 (catch java.io.FileNotFoundException e
                   {:err (str "File '" filename "' not found. " (.getMessage e))})
                 (catch java.io.IOException e
                   {:err (str "IO exception when reading file '" filename "', but the file was found. " (.getMessage e))}))] 
    (if (= (compare (str (type result)) "class clojure.lang.PersistentArrayMap") 0)
      (assoc response :reason (:err result))
      (assoc (assoc response :result result) :success true))))


(defn ^:impure write-file
  "Writes the string 'content' to file 'filename' and returns a map with the result.  Key 'success' is 'true' if successful, otherwise 'success' is 'false' and 'reason' contains the reason the operation failed."
  [filename content]
  (let [response {:success false}
        result (try
                 (spit filename content)
                 (catch java.io.FileNotFoundException e
                   (str "File '" filename "' not found. " (.getMessage e)))
                 (catch java.io.IOException e
                   (str "IO exception when writing file '" filename "'. " (.getMessage e))))]
    (if (nil? result)
      (assoc response :success true)
      (assoc response :reason result))))


(defn format-commit-msg-all
  "Performs overall formatting of the commit message--what can be applied to the entire message--with the message as a multi-line string 'commit-msg' and returns the formatted multi-line string as the result."
  [commit-msg]
  (-> commit-msg
      (str/replace #"(?m)^.*#.*" "")                                            ;; replace all lines that contain comments with empty strings
      (str/trim)                                                                ;; remove leading/trailing newlines/spaces
      (str/replace #"(?m)^[ ]+$" "")                                            ;; for a line with spaces only, remove all spaces
      (str/replace #"(?m)^\n{2,}" "\n")                                         ;; replace two or more consecutive newlines with a single newline
      (str/replace #"(?m)[ ]+$" "")                                            ;; remove spaces at end of lines (without removing spaces at beginning of lines)
      (str/replace #"^(.+)\n+(.)" "$1\n\n$2")                                   ;; ensure exactly two newlines between subject and body (if any body)
      (str/replace #"(?mi)BRE?AKING[ -_]*CHANGE[ ]*:[ ]*" "BREAKING CHANGE: ")  ;; convert to 'BREAKING CHANGE:' regardless of: case, mispelled 'BRAKING', separated with space/dash/underscore, and searpated by 0 or more spaces before and/or after the colon
      (str/replace #"(?mi)BRAEKING[ -_]*CHANGE[ ]*:[ ]*" "BREAKING CHANGE: ")   ;; as above, if mispelled 'BRAEKING'
      (str/trim)))                                                              ;; remove leading/trailing newlines/spaces (again)


(defn format-commit-msg-first-line
  "Performs formatting of the first line (e.g. subject line aka title line) only of the commit message and returns the formatted string result.  The 'line' must be a string of the first line only."
  [line]
  (-> line
      (str/trim)                     ;; remove spaces at beginning/end of line
      (str/replace #"[ ]*\(" "(")    ;; remove extra spaces before the opening parenthesis
      (str/replace #"\([ ]*" "(")    ;; remove extra spaces after the opening parenthesis
      (str/replace #"[ ]*\)" ")")    ;; remove extra spaces before the closing parenthesis
      (str/replace #"\)[ ]*" ")")    ;; remove extra spaces after the closing parenthesis
      (str/replace #"[ ]*!" "!")     ;; remove extra spaces before the exclamation mark
      (str/replace #"[ ]*:" ":")     ;; remove extra spaces before the colon
      (str/replace #":[ ]*" ": ")))  ;; replace no space or extra spaces after the colon with a single space


(defn format-commit-msg
  "Accepts a string commit-msg and returns the formatted string commit-message.  If the commit message is an empty string or nil, then returns an empty string."
  [commit-msg]
  (if (empty? commit-msg)
    ""
    (let [commit-msg-vec (split-lines (format-commit-msg-all commit-msg))]
      (str/join "\n" (into [] (concat (conj [] (format-commit-msg-first-line (first commit-msg-vec))) (rest commit-msg-vec)))))))


(defn index-matches
  "Returns a lazy sequence containing the zero-based indicies of matches found applying the 'regex' to the 'collection'.  If no matches, then the returned lazy sequence is empty."
  [collection regex]
  (keep-indexed (fn [idx itm] (when-not (empty? (re-find regex itm)) idx)) collection))


(defn create-validate-commit-msg-err
  "Creates and return a map describing a commit message validation error with key 'success' to 'false', 'reason', and optional 'locations'."
  ([reason]
   (create-validate-commit-msg-err reason nil))
  ([reason locations]
   (let [response (-> {}
                      (assoc :success false)
                      (assoc :reason reason))]
     (if (nil? locations)
       response
       (assoc response :locations locations)))))


(defn validate-commit-msg-title-len
  "Validates the commit message string 'title' (e.g. first line), returning 'nil' on success and a map on error with key 'success' equal to 'false', 'reason', and optional 'locations'.  The title is valid if it's within the min/max character range (inclusive) set in the config file."
  [title config]
  (if (seq (re-find (Pattern/compile (str "^.{" (:min (:title-line (:length (:commit-msg config)))) ",}$")) title))    ;; regex for n or more characters
    (if (seq (re-find (Pattern/compile (str "^.{1," (:max (:title-line (:length (:commit-msg config)))) "}$")) title)) ;; regex for n or fewer characters
      nil
      (create-validate-commit-msg-err (str "Commit message title line must not contain more than " (:max (:title-line (:length (:commit-msg config)))) " characters.") (lazy-seq [0])))
    (create-validate-commit-msg-err (str "Commit message title line must be at least " (:min (:title-line (:length (:commit-msg config)))) " characters.") (lazy-seq [0]))))


(defn validate-commit-msg-body-len
  "Validates the commit message 'body' (e.g. lines after the title) where each line of the body is an element of a vector; must not have an element representing the two newlines separating the title from the body. Returns 'nil' on success (including if 'body' is an empty sequence) and a map on error with key 'success' equal to 'false', 'reason', and optional 'locations'.  The body is valid if all lines are within the min/max character range (inclusive) set in the config file."
  [body config]
  (if (empty? body)
    nil
    (let [err-body-min (index-matches body (Pattern/compile (str "^.{1," (dec (:min (:body-line (:length (:commit-msg config))))) "}$")))]     ;; regex for n or more characters
      (if (= 0 (count err-body-min))
        (let [err-body-max (index-matches body (Pattern/compile (str "^.{" (inc (:max (:body-line (:length (:commit-msg config))))) ",}$")))]  ;; regex for n or fewer characters
          (if (= 0 (count err-body-max))
            nil
            (create-validate-commit-msg-err (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") err-body-max)))
        (create-validate-commit-msg-err (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") err-body-min)))))


(defn add-string-if-key-empty
  "Adds the 'add-text' to 'text' if the value in 'collection' identified by 'key' is empty and returns the modified text; else returns 'text' unchanged.  Adds two spaces before adding 'add-text' to 'text' if 'text' is not empty, else does not."
  [text add-text key collection]
  (if (empty? ((keyword key) collection))
    (if (empty? text)
      add-text
      (str text "  " add-text))
    text))


(defn validate-commit-msg-title-scope-type
  "Validates the commit message title line (as a string) for type, scope, and description but does NOT check type/scope against the config.  Returns a map result of key 'success' set to bool 'true' with string 'type', string 'scope', bool 'breaking' if breaking change or not, and string 'title-descr'.  Else returns key 'success' set to bool 'false' with string 'reason'."
  [title]
  (let [matcher (re-matcher #"^(?<type>[a-z]+)\((?<scope>([a-zA-Z0-9]+(.[a-zA-Z0-9]+)*))\)(?<breaking>!)?:(?<descr>.*)" title)]
    (if (.matches matcher)
      (let [match {:type (.group matcher "type")
                   :scope (.group matcher "scope")
                   :breaking (if (empty? (.group matcher "breaking"))
                               false
                               true)
                   :title-descr (str/trim (.group matcher "descr"))}
            reason (-> ""
                       (add-string-if-key-empty "Could not identify type." :type match)
                       (add-string-if-key-empty "Could not identify scope." :scope match)
                       (add-string-if-key-empty "Could not identify description." :title-descr match))]
        (if (empty? reason)
          (assoc match :success true)
          (create-validate-commit-msg-err (str "Bad form on title.  " reason) (lazy-seq [0]))))
      (create-validate-commit-msg-err "Bad form on title.  Could not identify type, scope, or description." (lazy-seq [0])))))


(defn get-scope
  "Returns the scope as a string if either scope or scope-alias in `node` match the `scope-query` else nil.  The `node` and `scope-query` must be valid.  The `node` can be a project or artifact."
  [scope-query node]
  (let [scope (:scope node)]
    (if (= scope scope-query)
      scope
      (let [scope-alias (:scope-alias node)]
        (if (= scope-alias scope-query)
          scope
          nil)))))


(defn get-scope-in-col
  "Searches for the `scope-query` in the collection `col` of maps, where the query could be found in ':scope' or ':scope-alias'.  Returns a map on success with key 'success' set to 'true', 'scope' set to the scope found even if the match was to a scope-alias, and 'index' as the zero-based index of the match in the collection.  Returns 'nil' if a match is not found."
  [scope-query col]
  (let [result (keep-indexed (fn [idx itm]
                               (let [result (get-scope scope-query itm)]
                                 (if (nil? result)
                                   nil
                                   {:success true
                                    :scope result
                                    :index idx}))) col)]
    (if (empty? result)
      {:success false}
      (first result))))


(defn get-scope-in-artifacts-or-projects
  "Finds the string `scope`, which can be a scope or scope alias, in the `node` ':artifacts' or ':projects' and returns a map result.  If found, returns key 'success' to boolean 'true', 'scope' to the string scope (even if the input scope was a scope alias), the `property` where the scope was found as either keyword ':artifacts' or ':projects', and `index` to the zero-based index in the sequence.  Otherwise returns boolean 'false'.  The `scope` and `node` must be valid."
  [scope node]
  (let [artifact-result (get-scope-in-col scope (:artifacts node))]
    (if (:success artifact-result)
      {:success true
       :scope (:scope artifact-result)
       :property :artifacts
       :index (:index artifact-result)}
      (let [project-result (get-scope-in-col scope (:projects node))]
        (if (:success project-result)
          {:success true
           :scope (:scope project-result)
           :property :projects
           :index (:index project-result)}
          {:success false})))))


(defn find-scope-path
  "Finds the scope and json paths for the string `query-path`, which can be a dot-separated path of scope and/or scope-aliases, using the `config` returning a map result.  If found, returns key 'success' to boolean 'true', 'scope-path' as a vector of strings of scopes (even if the `query-path` contained scope aliases), and the 'json-path' as a vector of the json path (using keywords and integer indicies) through the config.  The `config` must be valid."
  [query-path config]
  (let [query-path-vec-top (str/split query-path #"\.")
        scope-top (first query-path-vec-top)
        node-top (get-in config [:project])
        root-project-scope (get-scope scope-top node-top)]  ;; check top-level project outside of loop, since it's json path is ':project' singluar vs ':projects' plural for artifacts/sub-projects
    (if (nil? root-project-scope)
      (create-validate-commit-msg-err (str "Definition for scope or scope-alias in title line of '" scope-top "' at query path of '[:project]' not found in config.") (lazy-seq [0]))
      (loop [scope-path [root-project-scope]           ;; the scope path that has been found thus far
             json-path [:project]                      ;; the path to the current node, which consists of map keys and/or array indicies
             query-path-vec (rest query-path-vec-top)  ;; the query path of scopes/scope-aliases that need to resolved
             node node-top]                            ;; the current node on which to find the next scope, from 'artifacts' or 'projects'
        (if (= 0 (count query-path-vec))
          {:success true
           :scope-path scope-path
           :json-path json-path}
          (let [scope (first query-path-vec)
                result (get-scope-in-artifacts-or-projects scope node)]
            (if (:success result)
              (let [next-json-path (conj json-path (:property result) (:index result))]
                (recur (conj scope-path (:scope result)) next-json-path (rest query-path-vec) (get-in config next-json-path)))
              (create-validate-commit-msg-err (str "Definition for scope or scope-alias in title line of '" scope "' at query path of '" (conj json-path [:artifacts :projects]) "' not found in config.") (lazy-seq [0])))))))))


(defn validate-commit-msg
  "Valides the `commit-msg` using definitions in `config` and returns a map result.  If valid, returns key 'success' to boolean 'true', 'scope-path' as a vector of one or more string scopes, 'json-path' as a vector of one or more keywords or integer indicies through the config, 'type' as the string type of change, and 'breaking' as a boolean 'true' if a breaking chagne and 'false' otherwise.  Else returns 'success' as boolean 'false', a string 'reason' for the error, and a lazy sequence 'locations' indicating the line number where the error occurred if applicable.  Valid if the `commit-msg` is not nil or empty, does not contain tabs, the title line and body lines are within the min/max character range per `config`, the title line meets the format for the type/scope/description and breaking change, and type/scope combination is defined in `config`; does not check footer tokens.  The `commit-msg` must be formatted and the `config` must be valid." 
  [commit-msg config]
  (if (empty? commit-msg)
    (create-validate-commit-msg-err "Commit message cannot be empty.")
    (let [commit-msg-all-col (split-lines commit-msg)
          commit-msg-title (first commit-msg-all-col)
          commit-msg-body-col (rest (rest commit-msg-all-col))  ;; the two 'rest' operations get the body collection without the empty string created by the two newlines separating the title from the body, if there is a body
          err-tab-seq (index-matches commit-msg-all-col #"	")]
      (if (= 0 (count err-tab-seq))
        (let [err-title (validate-commit-msg-title-len commit-msg-title config)]
          (if (nil? err-title)
            (let [err-body (validate-commit-msg-body-len commit-msg-body-col config)]
              (if (nil? err-body)
                (let [scope-type-response (validate-commit-msg-title-scope-type commit-msg-title)]
                  (if (:success scope-type-response)
                    (let [scope-path-response (find-scope-path (:scope scope-type-response) config)]
                      (if (:success scope-path-response)
                        (let [types (get-in config (conj (:json-path scope-path-response) :types))]
                          (if (some (fn [itm] (= (:type scope-type-response) itm)) types)
                            {:success true
                             :scope-path (:scope-path scope-path-response)
                             :json-path (:json-path scope-path-response)
                             :type (:type scope-type-response)
                             :breaking (or (:breaking scope-type-response) (if (> (count (index-matches commit-msg-all-col #"BREAKING CHANGE:")) 0)
                                                                             true
                                                                             false))}
                            (create-validate-commit-msg-err (str "Definition in title line of type '" (:type scope-type-response) "' for scope '" (:scope scope-type-response) "' at query path of '" (:json-path scope-path-response) "' not found in config.") (lazy-seq [0]))))
                        scope-path-response))
                    scope-type-response))
                err-body))
            err-title))
        (create-validate-commit-msg-err "Commit message cannot contain tab characters." err-tab-seq)))))

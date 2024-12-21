#!/usr/bin/env bb

;; (c) Copyright 2023-2025 KineticFire. All rights reserved.
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


(ns semver-multi.common.file
  (:require [clojure.set                 :as set]
            [clojure.java.io             :as io]
            [cheshire.core               :as json]
            [semver-multi.common.version :as version]))



(defn ^:impure read-file
  "Reads the file 'filename' and returns a map with the result.  Key 'success' is 'true' if successful and 'result'
   contains the contents of the file as a string, otherwise 'success' is 'false' and 'reason' contains the reason the
   operation failed."
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
  "Writes the string 'content' to file 'filename' and returns a map with the result.  Key 'success' is 'true' if
   successful, otherwise 'success' is 'false' and 'reason' contains the reason the operation failed."
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


(defn ^:impure parse-json-file
  "Reads and parses the JSON config file, 'filename', and returns a map result.  If successful, ':success' is 'true' and
   'result' contains the JSON config as a map.  Else ':success' is 'false' and ':reason' describes the failure."
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


(defn ^:impure get-input-file-data
  "Loads and parses input file data defined for each key that is set, regardless of mode.  Loads and parses the data,
   looking for keys in 'params':
      ':project-def-file'-- the project definition file.  Parsed results returned in 'project-def-json'.
      'version-file'-- the version data file.  Parsed results returned in 'version-json'.
   Returns a map result with key ':success' of 'true' if all files were found, accessed, and parsed correctly (including
   if no files were specified) and includes key(s) for the parsed results of the file(s).  On error, returns key
   ':success' of 'false' and ':reason' with the reason for the failure."
  [params]
  (let [response (if (nil? (:project-def-file params))
                   {:success true}
                   (parse-json-file (:project-def-file params)))]
    (if-not (:success response)
      response
      (let [response (set/rename-keys response {:result :project-def-json})
            version-read-result (if (nil? (:version-file params))
                                  {}
                                  (read-file (:version-file params)))]
        (if (and (contains? version-read-result :success)
                 (not (:success version-read-result)))
          version-read-result
          (let [version-parse-result (if-not (contains? version-read-result :success)
                                       {}
                                       (version/parse-version-data (:result version-read-result)))]
            (if (and (contains? version-parse-result :success)
                     (not (:success version-parse-result)))
              version-parse-result
              (let [response (merge response version-parse-result)]
                response))))))))

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


(ns semver-multi.common.version
  (:require [clojure.string    :as str]
            [cheshire.core     :as json]))





(def ^:const version-data-marker-start "semver-multi_start")

(def ^:const version-data-marker-end "semver-multi_end")

(def ^:const version-type-keyword-to-string-map
  {:release      "release"
   :test-release "test-release"
   :update       "update"})


(defn version-type-keyword-to-string
  "Returns the string equivalent of the type keyword `type-keyword` for version generation or 'nil' if the
   `type-keyword` if not known."
  [type-keyword]
  (type-keyword version-type-keyword-to-string-map))



(defn parse-version-data
  "Parses JSON version data `data` surrounded by start and end markings (e.g., 'semver-multi_start' and 'semver-multi_end')
   and, on success, returns a map with key ':success' set to 'true' and ':version-json' set to the parsed JSON.  If
   the operation fails, then ':success' is ':false' and ':reason' describes the reason for failure."
  [data]
  (let [matcher (re-matcher #"(?is).*semver-multi_start(?<ver>.*)semver-multi_end.*" data)]
    (if-not (.matches matcher)
      {:success false
       :reason "Could not find start/end markers"}
      (let [version-string (str/trim (.group matcher "ver"))]
        (if (empty? version-string)
          {:success false
           :reason "Version data is empty"}
          (let [response {:success false}
                result (try
                         (json/parse-string version-string true)
                         (catch java.io.IOException e
                           (str "JSON parse error when parsing input data")))]
            (if (= (compare (str (type result)) "class clojure.lang.PersistentArrayMap") 0)
              (assoc (assoc response :version-json result) :success true)
              (assoc response :reason result))))))))
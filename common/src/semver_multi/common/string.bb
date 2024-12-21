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


(ns semver-multi.common.string
  (:require [clojure.string    :as str]))



(defn split-lines
  "Splits the string 'data' based on an optional carriage return '\r' and newline '\n' and returns the result as a
   vector.  Same as split-lines, but returns all newlines (including those that are newline-only)."
  [data]
  (str/split data #"\r?\n" -1))


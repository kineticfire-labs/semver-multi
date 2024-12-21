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


(ns semver-multi.common.collections)




(defn add-string-if-key-empty
  "Adds the 'add-text' to 'text' if the value in 'collection' identified by 'key' is empty and returns the modified
   text; else returns 'text' unchanged.  Adds two spaces before adding 'add-text' to 'text' if 'text' is not empty, else
   does not."
  [text add-text key collection]
  (if (empty? ((keyword key) collection))
    (if (empty? text)
      add-text
      (str text "  " add-text))
    text))


(defn get-frequency-on-properties-on-array-of-objects
  "Returns a map with the key as the element, found in the `target` sequence with objects for `properties`, and value as
   the number of occurrences of that element if the occurrences are two or greater."
  [target properties]
  (filter some? (map (fn [[key value]] (when (>= value 2) key)) (frequencies (apply concat (map (fn [path] (map (fn [project] (get-in project [path])) target)) properties))))))


(defn index-matches
  "Returns a lazy sequence containing the zero-based indices of matches found applying the 'regex' to the 'collection'.
   If no matches, then the returned lazy sequence is empty."
  [collection regex]
  (keep-indexed (fn [idx itm] (when-not (empty? (re-find regex itm)) idx)) collection))
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


(ns semver-multi.common.util
  (:import (java.util.regex Pattern)))



(defn do-on-success
  "Performs the function 'fn' if the last argument is a map with key 'success' is set to 'true', otherwise returns the
   last argument."
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


(defn contains-value?
  "Returns boolean 'true' if the value `val` is contained in the collection `col` and 'false' otherwise."
  [col val]
  (if (some #(= % val) col)
    true
    false))


(defn find-duplicates
  "Returns a vector of duplicates found in the collection 'v'.  If no duplicates, then returns an empty vector."
  [col]
  (let [frequencies (frequencies col)]
    (vec (keys (filter #(> (val %) 1) frequencies)))))


(defn duplicates?
  "Returns boolean 'true' if the collection `col` contains at least one duplicate and 'false' otherwise."
  [col]
  (if (> (count (find-duplicates col)) 0)
    true
    false))


(defn valid-string?
  "Validates the string `str`, returning boolean 'true' if valid and 'false' otherwise.  The string is valid if it is:
     - nil if `nil-ok` is 'true', else must not be nil
     - a string
     - greater than or equal in length to `min`
     - less than or equal in length to `max`
  "
  [nil-ok min max str]
  (if (nil? str)
    (if nil-ok
      true
      false)
    (if-not (string? str)
      false
      (let [length (count str)]
        (if (< length min)
          false
          (if (> length max)
            false
            true))))))


(defn valid-integer?
  "Validates the integer `int`, returning boolean 'true' if valid and 'false' otherwise.  The integer is valid if it is:
     - nil if `nil-ok` is 'true', else must not be nil
     - an integer
     - greater than or equal to `min`
     - less than or equal to `max`
  "
  [nil-ok min max int]
  (if (nil? int)
    (if nil-ok
      true
      false)
    (if-not (integer? int)
      false
      (if (< int min)
        false
        (if (> int max)
          false
          true)))))


;; todo finish
;; todo docs
;; the collection itself can't be nil
(defn valid-coll?
  [duplicates-ok min max fn coll]
  (if (nil? coll)
    false
    (if-not (coll? coll)
    false
    "todo")))


;; todo finish
;; todo docs.  include recommendation about using 'partial' function.
;; if map is nil, then false is required if false else true
(defn valid-map-entry?
  [key-path required nil-ok entry-type fn map]
  (let [entry (get-in map key-path :com-kineticfire-not-found)
        key-was-found (if (= :com-kineticfire-not-found entry)
                        false
                        true)]
    (if-not key-was-found
      (if required
        false
        true)
      (if (nil? entry)
        (if nil-ok
          true
          false)
        (if (= entry-type :scalar)
          (if (coll? entry)
            false
            (fn entry))
          "todo: if-not coll? then false")))))


;; todo:  account for build info
(defn is-semantic-version-release?
  "Returns 'true' if `version` is a valid semantic version for a release and 'false' otherwise."
  [version]
  (if (seq (re-find (Pattern/compile "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$") version))
    true
    false))
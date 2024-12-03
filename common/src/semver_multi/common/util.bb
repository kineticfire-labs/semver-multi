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
  (:require [clojure.set :as set])
  (:import (java.util.regex Pattern)))


(def ^:const valid-string-as-keyword-pattern (Pattern/compile (str "^[a-zA-Z][a-zA-Z0-9_-]*$")))

(def ^:const semantic-version-release-pattern (Pattern/compile "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$"))



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


(defn valid-coll?
  "Validates the collection `col`, returning boolean 'true' if valid else 'false'.  If `duplicates-ok` is set to 'true'
  then duplicates are allowed, else 'false' will be returned if the collection contains duplicates.  The collection must
  have at least `min` elements and at most `max` elements to be valid.  The collection itself can't be nil and must be
  a collection.

  The `fn` is a per-element evaluation function that must return boolean 'true' if valid and 'false' otherwise.  Look
  to functions 'valid-string?' and 'valid-integer?' to help validate the contents of the collection.  These functions
  can be passed to 'partial' then the output of that function used in this function."
  [duplicates-ok min max fn col]
  (if (nil? col)
    false
    (if-not (coll? col)
    false
    (let [num (count col)]
      (if (or
            (< num min)
            (> num max))
        false
        (if (and
              (not duplicates-ok)
              (duplicates? col))
          false
          (not (contains-value? (map fn col) false))))))))


(defn valid-map-entry?
  "Validates the entry in map `map`, returning boolean 'true' if valid else 'false'.  The entry in the map is identified
  by the key sequence `key-path`.  If the path does not exist (or if the map is 'nil' so the key sequence doesn't
  exist), then 'false' is returned unless `required` is set to 'true'.  If the value at the key sequence is 'nil', then
  'false' is returned unless 'nil-ok' is set to 'true'.

  The `fn` is an evaluation function that operates on the entry at the key sequence and must return boolean 'true' if
  valid and 'false' otherwise.  Look to functions 'valid-string?' and 'valid-integer?' for validating scalar values and
  'valid-coll?' for validating a collection.  These functions can be passed to 'partial' then the output of that
  function used in this function."
  [key-path required nil-ok fn map]
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
        (fn entry)))))


(defn valid-string-as-keyword?
  "Returns boolean 'true' if the string `str` would be valid keyword and 'false' otherwise.  If `nil-ok` is 'true', then
  the evaluation returns 'true' if `str` is 'nil'.  Otherwise, a valid keyword starts with a letter and includes only
  letters, numbers, dash, and underscore."
  [nil-ok str]
  (if (valid-string? nil-ok 1 Integer/MAX_VALUE str)
    (if (nil? str)
      true
      (if (seq (re-find valid-string-as-keyword-pattern str))
        true
        false))
    false))


(defn symmetric-difference-of-sets [set1 set2]
  "Returns the symmetric difference between `set1` and `set2`."
  (set/union (set/difference set1 set2) (set/difference set2 set1)))


(defn intersection-vec
  "Returns the intersection between two vectors `vec1` and `vec2` as a vector."
  [vec1 vec2]
  (vec (set/intersection (set vec1) (set vec2))))


(defn assoc-in
  "Associates a value in a nested associative structure.  If any levels do not exist, hash-maps will be created.

  For signature 'm ks v': associates the value `v` in the nested associative structure `m` as key sequence `ks`.  For
  signature 'm ks-v-col': behaves as above with `ks-v-coll` as a collection with one key sequence and one value."
  ([m ks-v-coll]
   (reduce (fn [acc single-ks-v]
             (let [ks (first single-ks-v)
                   v (last single-ks-v)]
               (clojure.core/assoc-in acc ks v)))
           m
           ks-v-coll))
  ([m ks v]
   (clojure.core/assoc-in m ks v)))


(defn dissoc-in
  "Disassociates a value in a nested associative structure `m`, where `ks` is either a sequence of keys or a collection
  of key sequences."
  [m ks]
  (if (empty? ks)
    (dissoc m)
    (if (coll? (first ks))
      (reduce (fn [acc single-key-seq]
                (dissoc-in acc single-key-seq))
              m
              ks)
      (if (= (count ks) 1)
        (dissoc m (first ks))
        (update-in m (butlast ks) dissoc (last ks))))))



(defn is-semantic-version-release?
  "Returns 'true' if `version` is a valid semantic version for a release and 'false' otherwise."
  [version]
  (if (seq (re-find semantic-version-release-pattern version))
    true
    false))
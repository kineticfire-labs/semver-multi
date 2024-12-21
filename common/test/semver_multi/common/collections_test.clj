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


;; KineticFire Labs
;;	  Project site:  https://github.com/kineticfire-labs/semver-multi


(ns semver-multi.common.collections-test
  (:require [clojure.test                    :refer [deftest is testing]]
            [clojure.string                  :as str]
            [babashka.classpath              :as cp]
            [semver-multi.common.collections :as col]))


(cp/add-classpath "./")



(deftest add-string-if-key-empty-test
  (testing "text empty, and collection value not empty"
    (let [v (col/add-string-if-key-empty "" "Added text." :data {:data "non empty"})]
      (is (string? v))
      (is (= "" v))))
  (testing "text empty, and collection value is not defined so would be empty"
    (let [v (col/add-string-if-key-empty "" "Added text." :other {:data "non empty"})]
      (is (string? v))
      (is (= "Added text." v))))
  (testing "text empty, and collection value is nil so is empty"
    (let [v (col/add-string-if-key-empty "" "Added text." :data {:data nil})]
      (is (string? v))
      (is (= "Added text." v))))
  (testing "text not empty, and collection value not empty"
    (let [v (col/add-string-if-key-empty "Original text." "Added text." :data {:data "non empty"})]
      (is (string? v))
      (is (= "Original text." v))))
  (testing "text not empty, and collection value is not defined so would be empty"
    (let [v (col/add-string-if-key-empty "Original text." "Added text." :other {:data "non empty"})]
      (is (string? v))
      (is (= "Original text.  Added text." v))))
  (testing "text not empty, and collection value is nil so is empty"
    (let [v (col/add-string-if-key-empty "Original text." "Added text." :data {:data nil})]
      (is (string? v))
      (is (= "Original text.  Added text." v)))))



(deftest get-frequency-on-properties-on-array-of-objects-test
  ;; single property
  (testing "empty target, no duplicates"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [] [:name])]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "single property, no duplicates"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a"} {:name "b"} {:name "c"}] [:name])]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "single property, 1 duplicate"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a"} {:name "b"} {:name "c"} {:name "c"}] [:name])]
      (is (seq? v))
      (is (= 1 (count v)))
      (is (= "c" (first v)))))
  (testing "single property, 2 duplicates"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a"} {:name "b"} {:name "c"} {:name "c"} {:name "b"}  {:name "b"}] [:name])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (some #{"b"} v))
      (is (some #{"c"} v))))
  ;; multiple properties
  (testing "multiple properties, no duplicates"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"}] [:name :other])]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "multiple properties, 1 duplicate on same property"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "c" :other "9"}] [:name :other])]
      (is (seq? v))
      (is (= 1 (count v)))
      (is (= "c" (first v)))))
  (testing "multiple properties, 2 duplicates on same properties"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "c" :other "9"} {:name "b" :other "8"}] [:name :other])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (some #{"b"} v))
      (is (some #{"c"} v))))
  (testing "multiple properties, 1 duplicate on different property"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "d" :other "3"}] [:name :other])]
      (is (seq? v))
      (is (= 1 (count v)))
      (is (= "3" (first v)))))
  (testing "multiple properties, 2 duplicates on different properties"
    (let [v (col/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "c" :other "9"} {:name "z" :other "2"}] [:name :other])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (some #{"2"} v))
      (is (some #{"c"} v)))))


(deftest index-matches-test
  (testing "empty collection"
    (let [v (col/index-matches [] #"z")]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "no matches"
    (let [v (col/index-matches ["aqq" "bqq" "cqq" "dqq"] #"z")]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "1 match with collection count 1"
    (let [v (col/index-matches ["bqq"] #"b")]
      (is (seq? v))
      (is (= 1 (count v)))))
  (testing "1 match with collection count 5"
    (let [v (col/index-matches ["aqq" "bqq" "cqq" "dqq"] #"a")]
      (is (seq? v))
      (is (= 1 (count v)))))
  (testing "4 matches with collection count 7"
    (let [v (col/index-matches ["aqq" "bqq" "acqq" "dqq" "eqq" "faqq" "gaqq"] #"a")]
      (is (seq? v))
      (is (= 4 (count v))))))
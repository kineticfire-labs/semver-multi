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


(ns semver-multi.common.util-test
  (:require [clojure.test             :refer [deftest is testing]]
            [babashka.classpath       :as cp]
            [semver-multi.common.util :as util]))


(cp/add-classpath "./")



(deftest do-on-success-test
  (testing "one arg: success"
    (let [v (util/do-on-success #(update % :val inc) {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 2 (:val v)))))
  (testing "one arg: fail"
    (let [v (util/do-on-success #(update % :val inc) {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "two args: success"
    (let [v (util/do-on-success #(assoc %2 :val (+ (:val %2) %1)) 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 6 (:val v)))))
  (testing "two args: fail"
    (let [v (util/do-on-success #(assoc %2 :val (+ (:val %2) %1)) 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "three args: success"
    (let [v (util/do-on-success #(assoc %3 :val (+ (:val %3) %1 %2)) 2 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 8 (:val v)))))
  (testing "three args: fail"
    (let [v (util/do-on-success #(assoc %3 :val (+ (:val %3) %1 %2)) 2 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "four args: success"
    (let [v (util/do-on-success #(assoc %4 :val (+ (:val %4) %1 %2 %3)) 1 2 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 9 (:val v)))))
  (testing "four args: fail"
    (let [v (util/do-on-success #(assoc %4 :val (+ (:val %4) %1 %2 %3)) 1 2 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v))))))


(defn perform-contains-value-test
  [col search fn-expected]
  (let [v (util/contains-value? col search)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest contains-value-test
  (testing "vector: contains"
    (perform-contains-value-test [1 2 3] 2 true?))
  (testing "vector: does not contain"
    (perform-contains-value-test [1 2 3] 0 false?))
  (testing "vector: empty"
    (perform-contains-value-test [] 2 false?))
  (testing "list: contains"
    (perform-contains-value-test '(1 2 3) 2 true?))
  (testing "list: does not contain"
    (perform-contains-value-test '(1 2 3) 0 false?))
  (testing "list: empty"
    (perform-contains-value-test '() 2 false?))
  (testing "set: contains"
    (perform-contains-value-test #{1 2 3} 2 true?))
  (testing "set: does not contain"
    (perform-contains-value-test #{1 2 3} 0 false?))
  (testing "set: empty"
    (perform-contains-value-test #{} 2 false?)))


(deftest find-duplicates-test
   (testing "no duplicates: empty vector"
    (let [v (util/find-duplicates [])]
       (is (vector? v))
       (is (= 0 (count v)))))
  (testing "no duplicates, integer: populated vector"
    (let [v (util/find-duplicates [1 2 3 10 4])]
       (is (vector? v))
       (is (= 0 (count v)))))
  (testing "no duplicates, string: populated vector"
    (let [v (util/find-duplicates ["alpha" "charlie" "bravo" "foxtrot" "kilo"])]
       (is (vector? v))
       (is (= 0 (count v)))))
  (testing "one duplicate, integer"
    (let [v (util/find-duplicates [1 2 3 10 3])]
       (is (vector? v))
       (is (= 1 (count v)))
       (util/contains-value? v 3)))
  (testing "one duplicate, string"
    (let [v (util/find-duplicates ["alpha" "charlie" "bravo" "alpha" "kilo"])]
       (is (vector? v))
       (is (= 1 (count v)))
       (util/contains-value? v "alpha")))
  (testing "three duplicates, integer"
    (let [v (util/find-duplicates [1 2 3 10 3 1 8 2])]
       (is (vector? v))
       (is (= 3 (count v)))
       (util/contains-value? v 3)
       (util/contains-value? v 1)
       (util/contains-value? v 2)))
  (testing "three duplicates, string"
    (let [v (util/find-duplicates ["alpha" "charlie" "bravo" "alpha" "kilo" "charlie" "bravo"])]
       (is (vector? v))
       (is (= 3 (count v)))
       (util/contains-value? v "alpha")
       (util/contains-value? v "charlie")
       (util/contains-value? v "bravo"))))


(defn perform-duplicates?-test
  [col fn-expected]
  (let [v (util/duplicates? col)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest duplicates?-test
  (testing "no duplicates: empty vector"
    (perform-duplicates?-test [] false?))
  (testing "no duplicates, integer: populated vector"
    (perform-duplicates?-test [1 2 3 10 4] false?))
  (testing "no duplicates, string: populated vector"
    (perform-duplicates?-test ["alpha" "charlie" "bravo" "foxtrot" "kilo"] false?))
  (testing "one duplicate, integer"
    (perform-duplicates?-test [1 2 3 10 3] true?))
  (testing "one duplicate, string"
    (perform-duplicates?-test ["alpha" "charlie" "bravo" "alpha" "kilo"] true?))
  (testing "three duplicates, integer"
    (perform-duplicates?-test [1 2 3 10 3 1 8 2] true?))
  (testing "three duplicates, string"
    (perform-duplicates?-test ["alpha" "charlie" "bravo" "alpha" "kilo" "charlie" "bravo"] true?)))


(defn perform-valid-string?-test
  [nil-ok min max fn-expected str]
  (let [v (util/valid-string? nil-ok min max str)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-string?-test
  (testing "invalid: nil w/ nil not ok"
    (perform-valid-string?-test false 1 5 false? nil))
  (testing "valid: nil w/ nil ok"
    (perform-valid-string?-test true 1 5 true? nil))
  (testing "invalid: not string, input number"
    (perform-valid-string?-test false 1 5 false? 5))
  (testing "invalid: not string, input vector of strings"
    (perform-valid-string?-test false 1 5 false? ["alpha" "bravo"]))
  (testing "invalid: less than min (empty string)"
    (perform-valid-string?-test false 1 5 false? ""))
  (testing "invalid: greater than max"
    (perform-valid-string?-test false 1 5 false? "abcdef"))
  (testing "valid: equal to min"
    (perform-valid-string?-test false 1 5 true? "a"))
  (testing "valid: greater than min"
    (perform-valid-string?-test false 1 5 true? "ab"))
  (testing "valid: equal to max"
    (perform-valid-string?-test false 1 5 true? "abcde"))
  (testing "valid: less than max"
    (perform-valid-string?-test false 1 5 true? "abcd")))


(defn perform-valid-integer?-test
  [nil-ok min max fn-expected int]
  (let [v (util/valid-integer? nil-ok min max int)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-integer?-test
  (testing "invalid: nil w/ nil not ok"
    (perform-valid-integer?-test false 1 5 false? nil))
  (testing "valid: nil w/ nil ok"
    (perform-valid-integer?-test true 1 5 true? nil))
  (testing "invalid: not integer, input string"
    (perform-valid-integer?-test false 1 5 false? "3"))
  (testing "invalid: not integer, input float"
    (perform-valid-integer?-test false 1 5 false? 0.0))
  (testing "invalid: not integer, input collection of integers"
    (perform-valid-integer?-test false 1 5 false? [1 2 3]))
  (testing "invalid: less than min"
    (perform-valid-integer?-test false 1 5 false? 0))
  (testing "invalid: greater than max"
    (perform-valid-integer?-test false 1 5 false? 6))
  (testing "valid: equal to min"
    (perform-valid-integer?-test false 1 5 true? 1))
  (testing "valid: greater than min"
    (perform-valid-integer?-test false 1 5 true? 2))
  (testing "valid: equal to max"
    (perform-valid-integer?-test false 1 5 true? 5))
  (testing "valid: less than max"
    (perform-valid-integer?-test false 1 5 true? 4)))


(defn perform-valid-map-entry?-test
  [key-path required nil-ok entry-type fn map fn-expected]
  (let [v (util/valid-map-entry? key-path required nil-ok entry-type fn map)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-map-entry?-test
  ;;
  ;; required and nil
  (testing "invalid: map nil so key-path not found w/ required true"
    (perform-valid-map-entry?-test [:a :b] true false :scalar (partial util/valid-string? false 1 5) nil false?))
  (testing "valid: map nil so key-path not found w/ required false"
    (perform-valid-map-entry?-test [:a :b] false false :scalar (partial util/valid-string? false 1 5) nil true?))
  (testing "invalid: key-path not found w/ required true"
    (perform-valid-map-entry?-test [:a :b] true false :scalar (partial util/valid-string? false 1 5) {:a {:c 1}} false?))
  (testing "valid: key-path not found w/ required false"
    (perform-valid-map-entry?-test [:a :b] false false :scalar (partial util/valid-string? false 1 5) {:a {:c 1}} true?))
  (testing "invalid: nil w/ nil not ok"
    (perform-valid-map-entry?-test [:a :b] true false :scalar (partial util/valid-string? false 1 5) {:a {:b nil}} false?))
  (testing "valid: nil w/ nil ok"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b nil}} true?))
  ;;
  ;; scalar, using 'valid-string/' function
  (testing "invalid: not string, input number"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b 1}} false?))
  (testing "invalid: not string, input vector of strings"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b ["alpha" "bravo"]}} false?))
  (testing "invalid: string length less than min (empty string)"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b ""}} false?))
  (testing "invalid: string length greater than max"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b "abcdef"}} false?))
  (testing "valid: string length equal to min"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b "a"}} true?))
  (testing "valid: string length equal to max"
    (perform-valid-map-entry?-test [:a :b] true true :scalar (partial util/valid-string? false 1 5) {:a {:b "abcde"}} true?))
  )


(defn perform-is-semantic-version-release?
  [version result]
  (let [v (util/is-semantic-version-release? version)]
    (is (boolean? v))
    (is (= v result))))


(deftest is-semantic-version-release?-test
  (testing "valid all 0s"
    (perform-is-semantic-version-release? "0.0.0" true))
  (testing "valid with 0s for minor and patch"
    (perform-is-semantic-version-release? "1.0.0" true))
  (testing "valid: no 0s"
    (perform-is-semantic-version-release? "1.2.3" true))
  (testing "valid: multiple digits"
    (perform-is-semantic-version-release? "123.456.7891" true))
  (testing "invalid: major leading 0"
    (perform-is-semantic-version-release? "01.0.0" false))
  (testing "invalid: minor leading 0"
    (perform-is-semantic-version-release? "1.01.0" false))
  (testing "invalid: patch leading 0"
    (perform-is-semantic-version-release? "1.0.01" false))
  (testing "invalid: major has alpha"
    (perform-is-semantic-version-release? "1a.0.0" false))
  (testing "invalid: minor has alpha"
    (perform-is-semantic-version-release? "1.0a.0" false))
  (testing "invalid: patch has alpha"
    (perform-is-semantic-version-release? "1.0.0a" false))
  (testing "invalid: not a release"
    (perform-is-semantic-version-release? "1.0.0-alpha.beta" false)))
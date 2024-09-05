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
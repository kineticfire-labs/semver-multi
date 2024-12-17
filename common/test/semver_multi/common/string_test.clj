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


;; KineticFire Labs: https://labs.kineticfire.com/
;;	   Project site: https://github.com/kineticfire-labs/semver-multi/


(ns semver-multi.common.string-test
  (:require [clojure.test               :refer [deftest is testing]]
            [babashka.classpath         :as cp]
            [semver-multi.common.string :as cstr]))


(cp/add-classpath "./")



(deftest split-lines-test
  (testing "empty string"
    (let [v (cstr/split-lines "")]
      (is (= 1 (count v)))
      (is (= "" (first v)))
      (is (vector? v))))
  (testing "single line"
    (let [v (cstr/split-lines "One long line")]
      (is (= 1 (count v)))
      (is (= "One long line" (first v)))
      (is (vector? v))))
  (testing "multiple lines"
    (let [v (cstr/split-lines "First line\nSecond line\nThird line")]
      (is (= 3 (count v)))
      (is (= "First line" (first v)))
      (is (= "Second line" (nth v 1)))
      (is (= "Third line" (nth v 2)))
      (is (vector? v)))))
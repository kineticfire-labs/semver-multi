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


(ns semver-multi.common.version-test
  (:require [clojure.test                :refer [deftest is testing]]
            [babashka.classpath          :as cp]
            [semver-multi.common.version :as version]))


(cp/add-classpath "./")



(deftest version-type-keyword-to-string-test
  (testing "not known"
    (is (nil? (version/version-type-keyword-to-string :not-known))))
  (testing "release"
    (is (= "release" (version/version-type-keyword-to-string :release))))
  (testing "test-release"
    (is (= "test-release" (version/version-type-keyword-to-string :test-release))))
  (testing "update"
    (is (= "update" (version/version-type-keyword-to-string :update)))))


(deftest parse-version-data-test
  (testing "fail: no start/end markers"
    (let [data "arg\n something something\n blah"
          v (version/parse-version-data data)]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Could not find start/end markers" (:reason v)))))
  (testing "fail: start marker but no end marker"
    (let [data "arg\n some semver-multi_start something\n blah"
          v (version/parse-version-data data)]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Could not find start/end markers" (:reason v)))))
  (testing "fail: empty"
    (let [data "arg\n some semver-multi_start semver-multi_end\n blah"
          v (version/parse-version-data data)]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Version data is empty" (:reason v)))))
  (testing "success"
    (let [data "arg\n some semver-multi_start\n {\"a\": \"alpha\", \"b\": \"bravo\"} semver-multi_end\n blah"
          v (version/parse-version-data data)]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:a (:version-json v)))))))

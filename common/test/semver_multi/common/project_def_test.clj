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


(ns semver-multi.common.project-def-test
  (:require [clojure.test                    :refer [deftest is testing]]
            [babashka.classpath              :as cp]
            [semver-multi.common.project-def :as proj]))


(cp/add-classpath "./")



;;
;; section: query the project definition
;;


(deftest get-scope-from-scope-or-alias-test
  (testing "scope not found and no scope-alias defined"
    (is (= (common/get-scope-from-scope-or-alias "alpha" {:scope "bravo"}) nil)))
  (testing "scope not found and scope-alias defined"
    (is (= (common/get-scope-from-scope-or-alias "alpha" {:scope "bravo" :scope-alias "charlie"}) nil)))
  (testing "scope found and no scope-alias defined"
    (is (= (common/get-scope-from-scope-or-alias "alpha" {:scope "alpha"}) "alpha")))
  (testing "scope found and scope-alias defined"
    (is (= (common/get-scope-from-scope-or-alias "alpha" {:scope "alpha" :scope-alias "bravo"}) "alpha")))
  (testing "scope-alias found"
    (is (= (common/get-scope-from-scope-or-alias "bravo" {:scope "alpha" :scope-alias "bravo"}) "alpha"))))


(deftest get-name-test
  (testing "single node, name found"
    (is (= (common/get-name {:name "alpha"}) "alpha")))
  (testing "single node, name not found"
    (is (nil? (common/get-name {:different "alpha"}))))
  (testing "multiple nodes, name found"
    (is (= (common/get-name {:top {:name "alpha"}} [:top]) "alpha")))
  (testing "multiple nodes, name not found"
    (is (nil? (common/get-name {:top {:different "alpha"}} [:top])))))

(deftest get-scope-test
  (testing "single node, scope found"
    (is (= (common/get-scope {:scope "alpha"}) "alpha")))
  (testing "single node, scope not found"
    (is (nil? (common/get-scope {:different "alpha"}))))
  (testing "multiple nodes, scope found"
    (is (= (common/get-scope {:top {:scope "alpha"}} [:top]) "alpha")))
  (testing "multiple nodes, scope not found"
    (is (nil? (common/get-scope {:top {:different "alpha"}} [:top])))))


(deftest get-scope-alias-else-scope-test
  (testing "single node, no scope-alias or scope, so return nil"
    (is (nil? (common/get-scope-alias-else-scope {:different "alpha"}))))
  (testing "single node, no scope-alias, so return scope"
    (is (= (common/get-scope-alias-else-scope {:scope "alpha"}) "alpha")))
  (testing "single node, scope-alias, so return scope-alias"
    (is (= (common/get-scope-alias-else-scope {:scope "alpha" :scope-alias "bravo"}) "bravo")))
  (testing "multi node, no scope-alias or scope, so return nil"
    (is (nil? (common/get-scope-alias-else-scope {:top {:different "alpha"}} [:top]))))
  (testing "multi node, no scope-alias, so return scope"
    (is (= (common/get-scope-alias-else-scope {:top {:scope "alpha"}} [:top]) "alpha")))
  (testing "multi node, scope-alias, so return scope-alias"
    (is (= (common/get-scope-alias-else-scope {:top {:scope "alpha" :scope-alias "bravo"}} [:top]) "bravo"))))


(deftest get-scope-in-col-test
  (testing "not found: empty collection"
    (let [v (common/get-scope-in-col "alpha" [])]
      (is (map? v))
      (is (false? (:success v)))
      (is (boolean? (:success v)))))
  (testing "not found: non-empty collection"
    (let [v (common/get-scope-in-col "alpha" [{:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}])]
      (is (map? v))
      (is (false? (:success v)))
      (is (boolean? (:success v)))))
  (testing "found: using scope, collection of 1"
    (let [v (common/get-scope-in-col "alpha" [{:scope "alpha"}])]
      (is (map? v))
      (is (true? (:success v)))
      (is (boolean? (:success v)))))
  (testing "found: using scope, collection of > 1"
    (let [v (common/get-scope-in-col "bravo" [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}])]
      (is (map? v))
      (is (true? (:success v)))
      (is (boolean? (:success v)))))
  (testing "found: using scope-alias, collection of 1"
    (let [v (common/get-scope-in-col "a" [{:scope "alpha" :scope-alias "a"}])]
      (is (map? v))
      (is (true? (:success v)))
      (is (boolean? (:success v)))))
  (testing "found: using scope-alias, collection of > 1"
    (let [v (common/get-scope-in-col "b" [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}])]
      (is (map? v))
      (is (true? (:success v)))
      (is (boolean? (:success v))))))




(deftest get-scope-in-artifacts-or-projects-test
  (testing "not found: no artifacts or projects"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))))
  (testing "not found: empty artifacts"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {:artifacts []})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))))
  (testing "not found: empty projects"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {:projects []})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))))
  (testing "not found: non-empty collection"
    (let [v (common/get-scope-in-artifacts-or-projects "zulu" {:artifacts [{:scope "alpha"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))))
  (testing "found: in artifacts using scope, collection of 1"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {:artifacts [{:scope "alpha" :scope-alias "a"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :artifacts (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in artifacts using scope, collection of > 1"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {:artifacts [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :artifacts (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in artifacts using scope-alias, collection of 1"
    (let [v (common/get-scope-in-artifacts-or-projects "a" {:artifacts [{:scope "alpha" :scope-alias "a"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :artifacts (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in artifacts using scope-alias, collection of > 1"
    (let [v (common/get-scope-in-artifacts-or-projects "a" {:artifacts [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :artifacts (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in projects using scope, collection of 1"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {:projects [{:scope "alpha" :scope-alias "a"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :projects (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in projects using scope, collection of > 1"
    (let [v (common/get-scope-in-artifacts-or-projects "alpha" {:projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :projects (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in projects using scope-alias, collection of 1"
    (let [v (common/get-scope-in-artifacts-or-projects "a" {:projects [{:scope "alpha" :scope-alias "a"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :projects (:property v)))
      (is (= 0 (:index v)))))
  (testing "found: in projects using scope-alias, collection of > 1"
    (let [v (common/get-scope-in-artifacts-or-projects "a" {:projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "alpha" (:scope v)))
      (is (= :projects (:property v)))
      (is (= 0 (:index v))))))


(deftest find-scope-path-test
  (testing "not found: project root"
    (let [v (common/find-scope-path "zulu" {:project {:scope "top"
                                                      :scope-alias "t"}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Definition for scope or scope-alias in title line of 'zulu' at query path of '[:project]' not found in config." (:reason v)))))
  (testing "found: project root as scope"
    (let [v (common/find-scope-path "top" {:project {:scope "top"
                                                     :scope-alias "t"}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 1 (count (:scope-path v))))
      (is (= "top" (first (:scope-path v))))
      (is (= 1 (count (:json-path v))))
      (is (= :project (first (:json-path v))))))
  (testing "found: project root as scope alias"
    (let [v (common/find-scope-path "t" {:project {:scope "top"
                                                   :scope-alias "t"}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 1 (count (:scope-path v))))
      (is (= "top" (first (:scope-path v))))
      (is (= 1 (count (:json-path v))))
      (is (= :project (first (:json-path v))))))
  (testing "not found: 2nd level node"
    (let [v (common/find-scope-path "top.zulu" {:project {:scope "top"
                                                          :scope-alias "t"
                                                          :artifacts [{:scope "art1" :scope-alias "a1"} {:scope "art2" :scope-alias "a2"} {:scope "art3" :scope-alias "a3"} {:scope "art4" :scope-alias "a4"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Definition for scope or scope-alias in title line of 'zulu' at query path of '[:project [:artifacts :projects]]' not found in config." (:reason v)))))
  (testing "found: root artifact as scope at first index"
    (let [v (common/find-scope-path "top.art1" {:project {:scope "top"
                                                          :scope-alias "t"
                                                          :artifacts [{:scope "art1" :scope-alias "a1"} {:scope "art2" :scope-alias "a2"} {:scope "art3" :scope-alias "a3"} {:scope "art4" :scope-alias "a4"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "art1" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :artifacts (nth (:json-path v) 1)))
      (is (= 0 (nth (:json-path v) 2)))))
  (testing "found: root artifact as scope at second index"
    (let [v (common/find-scope-path "top.art2" {:project {:scope "top"
                                                          :scope-alias "t"
                                                          :artifacts [{:scope "art1" :scope-alias "a1"} {:scope "art2" :scope-alias "a2"} {:scope "art3" :scope-alias "a3"} {:scope "art4" :scope-alias "a4"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "art2" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :artifacts (nth (:json-path v) 1)))
      (is (= 1 (nth (:json-path v) 2)))))
  (testing "found: root artifact as scope-alias at first index"
    (let [v (common/find-scope-path "top.a1" {:project {:scope "top"
                                                        :scope-alias "t"
                                                        :artifacts [{:scope "art1" :scope-alias "a1"} {:scope "art2" :scope-alias "a2"} {:scope "art3" :scope-alias "a3"} {:scope "art4" :scope-alias "a4"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "art1" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :artifacts (nth (:json-path v) 1)))
      (is (= 0 (nth (:json-path v) 2)))))
  (testing "found: root artifact as scope-alias at second index"
    (let [v (common/find-scope-path "top.a2" {:project {:scope "top"
                                                        :scope-alias "t"
                                                        :artifacts [{:scope "art1" :scope-alias "a1"} {:scope "art2" :scope-alias "a2"} {:scope "art3" :scope-alias "a3"} {:scope "art4" :scope-alias "a4"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "art2" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :artifacts (nth (:json-path v) 1)))
      (is (= 1 (nth (:json-path v) 2)))))
  (testing "found: 1 sub-project as scope at first index"
    (let [v (common/find-scope-path "top.alpha" {:project {:scope "top"
                                                           :scope-alias "t"
                                                           :projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "alpha" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :projects (nth (:json-path v) 1)))
      (is (= 0 (nth (:json-path v) 2)))))
  (testing "found: 1 sub-project as scope at second index"
    (let [v (common/find-scope-path "top.bravo" {:project {:scope "top"
                                                           :scope-alias "t"
                                                           :projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "bravo" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :projects (nth (:json-path v) 1)))
      (is (= 1 (nth (:json-path v) 2)))))
  (testing "found: 1 sub-project as scope-alias at first index"
    (let [v (common/find-scope-path "top.a" {:project {:scope "top"
                                                       :scope-alias "t"
                                                       :projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "alpha" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :projects (nth (:json-path v) 1)))
      (is (= 0 (nth (:json-path v) 2)))))
  (testing "found: 1 sub-project as scope-alias at second index"
    (let [v (common/find-scope-path "top.b" {:project {:scope "top"
                                                       :scope-alias "t"
                                                       :projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b"} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 2 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "bravo" (nth (:scope-path v) 1)))
      (is (= 3 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :projects (nth (:json-path v) 1)))
      (is (= 1 (nth (:json-path v) 2)))))
  (testing "found: 2 sub-projects as alternating scope/scope-alias at second index"
    (let [v (common/find-scope-path "top.b.sub" {:project {:scope "top"
                                                           :scope-alias "t"
                                                           :projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b" :projects [{:scope "sub"}]} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 3 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "bravo" (nth (:scope-path v) 1)))
      (is (= "sub" (nth (:scope-path v) 2)))
      (is (= 5 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :projects (nth (:json-path v) 1)))
      (is (= 1 (nth (:json-path v) 2)))
      (is (= :projects (nth (:json-path v) 3)))
      (is (= 0 (nth (:json-path v) 4)))))
  (testing "found: 1 sub-project and 1 artifact as alternating scope/scope-alias at second index"
    (let [v (common/find-scope-path "top.b.sub" {:project {:scope "top"
                                                           :scope-alias "t"
                                                           :projects [{:scope "alpha" :scope-alias "a"} {:scope "bravo" :scope-alias "b" :artifacts [{:scope "sub"}]} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= 3 (count (:scope-path v))))
      (is (= "top" (nth (:scope-path v) 0)))
      (is (= "bravo" (nth (:scope-path v) 1)))
      (is (= "sub" (nth (:scope-path v) 2)))
      (is (= 5 (count (:json-path v))))
      (is (= :project (nth (:json-path v) 0)))
      (is (= :projects (nth (:json-path v) 1)))
      (is (= 1 (nth (:json-path v) 2)))
      (is (= :artifacts (nth (:json-path v) 3)))
      (is (= 0 (nth (:json-path v) 4))))))



(deftest get-child-nodes-test
  ;; no child nodes, e.g. no projects or artifacts
  (testing "no child nodes"
    (let [v (common/get-child-nodes {} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 0 (count v)))))
  ;; projects but no artifacts
  (testing "one project, no artifacts"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 0] (:json-path (nth v 0))))))
  (testing "two projects, no artifacts"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"} {:name "proj2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 1] (:json-path (nth v 0))))
      (is (= "alpha" (:a (nth v 1))))
      (is (= [:project :projects 0] (:json-path (nth v 1))))))
  ;; artifacts but no projects
  (testing "one artifact, no projects"
    (let [v (common/get-child-nodes {:artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :artifacts 0] (:json-path (nth v 0))))))
  (testing "two artifacts, no projects"
    (let [v (common/get-child-nodes {:artifacts [{:name "art1"} {:name "art2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :artifacts 1] (:json-path (nth v 0))))
      (is (= "alpha" (:a (nth v 1))))
      (is (= [:project :artifacts 0] (:json-path (nth v 1))))))
  ;; projects and artifacts
  (testing "one project and one artifact"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"}] :artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 0] (:json-path (nth v 0))))
      (is (= [:project :artifacts 0] (:json-path (nth v 1))))))
  (testing "one project and one artifact"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"}] :artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 0] (:json-path (nth v 0))))
      (is (= [:project :artifacts 0] (:json-path (nth v 1))))))
  (testing "two projects and one artifact"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"} {:name "proj2"}] :artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 1] (:json-path (nth v 0))))
      (is (= [:project :projects 0] (:json-path (nth v 1))))
      (is (= [:project :artifacts 0] (:json-path (nth v 2))))))
  (testing "one project and two artifacts"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"}] :artifacts [{:name "art1"} {:name "art2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 0] (:json-path (nth v 0))))
      (is (= [:project :artifacts 1] (:json-path (nth v 1))))
      (is (= [:project :artifacts 0] (:json-path (nth v 2))))))
  (testing "one project and two artifacts"
    (let [v (common/get-child-nodes {:projects [{:name "proj1"} {:name "proj2"}] :artifacts [{:name "art1"} {:name "art2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 4 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= [:project :projects 1] (:json-path (nth v 0))))
      (is (= [:project :projects 0] (:json-path (nth v 1))))
      (is (= [:project :artifacts 1] (:json-path (nth v 2))))
      (is (= [:project :artifacts 0] (:json-path (nth v 3)))))))




(def get-depends-on-test-config
  {:project {:name "top"
             :scope "top"
             :projects [{:name "a"
                         :description "Project A"
                         :scope "alpha"
                         :scope-alias "a"}
                        {:name "b"
                         :description "Project B"
                         :scope "bravo"
                         :scope-alias "b"}
                        {:name "c"
                         :description "Project C"
                         :scope "charlie"
                         :scope-alias "c"}]
             :artifacts [{:name "Artifact X"
                          :description "Artifact X"
                          :scope "artx"
                          :scope-alias "x"}
                         {:name "Artifact Y"
                          :description "Artifact Y"
                          :scope "arty"
                          :scope-alias "y"}
                         {:name "Artifact Z"
                          :description "Artifact Z"
                          :scope "artz"
                          :scope-alias "z"}]}})


(deftest get-depends-on-test
  (testing "depends-on not defined"
    (let [node (get-in get-depends-on-test-config [:project :artifacts 0])
          v (common/get-depends-on node get-depends-on-test-config)]
      (is (= (count v) 0))))
  (testing "depends-on defined but empty"
    (let [config (assoc-in get-depends-on-test-config [:project :artifacts 0 :depends-on] [])
          node (get-in config [:project :artifacts 0])
          v (common/get-depends-on node config)]
      (is (= (count v) 0))))
  (testing "succes: 1 scope query path"
    (let [config (assoc-in get-depends-on-test-config [:project :artifacts 0 :depends-on] ["top.artx"])
          node (get-in config [:project :artifacts 0])
          v (common/get-depends-on node config)]
      (is (= (:json-path (first v)) [:project :artifacts 0]))
      (is (= (:scope-path (first v)) ["top" "artx"]))))
  (testing "succes: 2 scope query paths"
    (let [config (assoc-in get-depends-on-test-config [:project :artifacts 0 :depends-on] ["top.artx" "top.arty"])
          node (get-in config [:project :artifacts 0])
          v (common/get-depends-on node config)]
      (is (= (:json-path (first v)) [:project :artifacts 0]))
      (is (= (:scope-path (first v)) ["top" "artx"]))
      (is (= (:json-path (nth v 1)) [:project :artifacts 1]))
      (is (= (:scope-path (nth v 1)) ["top" "arty"])))))


(def get-child-nodes-including-depends-on-test-config
  {:project {:name "top"
             :full-json-path [:project]
             :full-scope-path ["top"]
             :full-scope-path-formatted "top"
             :scope "top"
             :projects [{:name "a"
                         :description "Project A"
                         :full-json-path [:project :projects 0]
                         :full-scope-path ["top" "alpha"]
                         :full-scope-path-formatted "top.alpha"
                         :scope "alpha"
                         :scope-alias "a"}
                        {:name "b"
                         :description "Project B"
                         :full-json-path [:project :projects 1]
                         :full-scope-path ["top" "bravo"]
                         :full-scope-path-formatted "top.bravo"
                         :scope "bravo"
                         :scope-alias "b",
                         :projects [{:name "bb"
                                     :description "Project BB"
                                     :full-json-path [:project :projects 1 :projects 0]
                                     :full-scope-path ["top" "bravo" "bravo2"]
                                     :full-scope-path-formatted "top.bravo.bravo2"
                                     :scope "bravo2"
                                     :scope-alias "b2"}]}
                        {:name "c"
                         :description "Project C"
                         :full-json-path [:project :projects 2]
                         :full-scope-path ["top" "charlie"]
                         :full-scope-path-formatted "top.charlie"
                         :scope "charlie"
                         :scope-alias "c"
                         :artifacts [{:name "Artifact C from X"
                                      :description "Artifact C from X"
                                      :full-json-path [:project :projects 2 :artifacts 0]
                                      :full-scope-path ["top" "charlie" "artcfrx"]
                                      :full-scope-path-formatted "top.charlie.artcfrx"
                                      :scope "artcfrx"
                                      :scope-alias "cfrx"}]}
                        {:name "d"
                         :description "Project D"
                         :full-json-path [:project :projects 3]
                         :full-scope-path ["top" "delta"]
                         :full-scope-path-formatted "top.delta"
                         :scope "delta"
                         :scope-alias "d"
                         :depends-on ["top.alpha" "top.bravo"]
                         :projects [{:name "Project D1"
                                     :description "Project D1"
                                     :full-json-path [:project :projects 3 :projects 0]
                                     :full-scope-path ["top" "delta" "d1"]
                                     :full-scope-path-formatted "top.delta.d1"
                                     :scope "d1"
                                     :scope-alias "d1"}
                                    {:name "Project D2"
                                     :description "Project D2"
                                     :full-json-path [:project :projects 3 :projects 1]
                                     :full-scope-path ["top" "delta" "d2"]
                                     :full-scope-path-formatted "top.delta.d2"
                                     :scope "d2"
                                     :scope-alias "d2"}]
                         :artifacts [{:name "Artifact AD1"
                                      :description "Artifact AD1"
                                      :full-json-path [:project :projects 3 :artifacts 0]
                                      :full-scope-path ["top" "delta" "ad1"]
                                      :full-scope-path-formatted "top.delta.ad1"
                                      :scope "ad1"
                                      :scope-alias "ad1"}
                                     {:name "Artifact AD2"
                                      :description "Artifact AD2"
                                      :full-json-path [:project :projects 3 :artifacts 1]
                                      :full-scope-path ["top" "delta" "ad2"]
                                      :full-scope-path-formatted "top.delta.ad2"
                                      :scope "ad2"
                                      :scope-alias "ad2"}]}]
             :artifacts [{:name "Artifact X"
                          :description "Artifact X"
                          :full-json-path [:project :artifacts 0]
                          :full-scope-path ["top" "artx"]
                          :full-scope-path-formatted "top.artx"
                          :scope "artx"
                          :scope-alias "x"}
                         {:name "Artifact Y"
                          :description "Artifact Y"
                          :full-json-path [:project :artifacts 1]
                          :full-scope-path ["top" "arty"]
                          :full-scope-path-formatted "top.arty"
                          :scope "arty"
                          :scope-alias "y"}
                         {:name "Artifact Z"
                          :full-json-path [:project :artifacts 2]
                          :full-scope-path ["top" "artz"]
                          :full-scope-path-formatted "top.artz"
                          :description "Artifact Z"
                          :scope "artz"
                          :scope-alias "z"}]}})


(deftest get-child-nodes-including-depends-on-test
  (testing "no children or depends-on"
    (let [node (get-in get-child-nodes-including-depends-on-test-config [:project :projects 0])
          v (common/get-child-nodes-including-depends-on node get-child-nodes-including-depends-on-test-config)]
      (is (= (count v) 0))))
  ;;
  ;; single child project/artifact
  (testing "one child: projects"
    (let [node (get-in get-child-nodes-including-depends-on-test-config [:project :projects 1])
          v (common/get-child-nodes-including-depends-on node get-child-nodes-including-depends-on-test-config)]
      (is (= (count v) 1))
      (is (= (:full-json-path (first v)) [:project :projects 1 :projects 0]))
      (is (= (:full-scope-path (first v)) ["top" "bravo" "bravo2"]))
      (is (= (:full-scope-path-formatted (first v)) "top.bravo.bravo2"))))
  (testing "one child: artifacts"
    (let [node (get-in get-child-nodes-including-depends-on-test-config [:project :projects 2])
          v (common/get-child-nodes-including-depends-on node get-child-nodes-including-depends-on-test-config)]
      (is (= (count v) 1))
      (is (= (:full-json-path (first v)) [:project :projects 2 :artifacts 0]))
      (is (= (:full-scope-path (first v)) ["top" "charlie" "artcfrx"]))
      (is (= (:full-scope-path-formatted (first v)) "top.charlie.artcfrx"))))
  ;;
  ;; single depends-on
  (testing "one depends-on"
    (let [config (assoc-in get-child-nodes-including-depends-on-test-config [:project :projects 0 :depends-on] ["top.bravo.bravo2"])
          node (get-in config [:project :projects 0])
          v (common/get-child-nodes-including-depends-on node config)]
      (is (= (count v) 1))
      (is (= (:full-json-path (first v)) [:project :projects 1 :projects 0]))
      (is (= (:full-scope-path (first v)) ["top" "bravo" "bravo2"]))
      (is (= (:full-scope-path-formatted (first v)) "top.bravo.bravo2"))))
  ;;
  ;; two each of project, artifact, depends-on
  (testing "two each of: project, artifact, depends-on"
    (let [node (get-in get-child-nodes-including-depends-on-test-config [:project :projects 3])
          v (common/get-child-nodes-including-depends-on node get-child-nodes-including-depends-on-test-config)]
      (is (= (count v) 6))
      ;; artifacts
      (is (= (:full-json-path (nth v 0)) [:project :projects 3 :artifacts 0]))
      (is (= (:full-scope-path (nth v 0)) ["top" "delta" "ad1"]))
      (is (= (:full-scope-path-formatted (nth v 0)) "top.delta.ad1"))
      (is (= (:full-json-path (nth v 1)) [:project :projects 3 :artifacts 1]))
      (is (= (:full-scope-path (nth v 1)) ["top" "delta" "ad2"]))
      (is (= (:full-scope-path-formatted (nth v 1)) "top.delta.ad2"))
      ;; projects
      (is (= (:full-json-path (nth v 2)) [:project :projects 3 :projects 0]))
      (is (= (:full-scope-path (nth v 2)) ["top" "delta" "d1"]))
      (is (= (:full-scope-path-formatted (nth v 2)) "top.delta.d1"))
      (is (= (:full-json-path (nth v 3)) [:project :projects 3 :projects 1]))
      (is (= (:full-scope-path (nth v 3)) ["top" "delta" "d2"]))
      (is (= (:full-scope-path-formatted (nth v 3)) "top.delta.d2"))
      ;; depends-on
      (is (= (:full-json-path (nth v 4)) [:project :projects 0]))
      (is (= (:full-scope-path (nth v 4)) ["top" "alpha"]))
      (is (= (:full-scope-path-formatted (nth v 4)) "top.alpha"))
      (is (= (:full-json-path (nth v 5)) [:project :projects 1]))
      (is (= (:full-scope-path (nth v 5)) ["top" "bravo"]))
      (is (= (:full-scope-path-formatted (nth v 5)) "top.bravo")))))



(deftest get-all-scopes-from-colection-of-artifacts-projects-test
  (testing "empty: not defined"
    (let [config {:something {}}
          json-path-vector [:something :projects]
          v (common/get-all-scopes-from-collection-of-artifacts-projects config json-path-vector)]
      (is (vector? v))
      (is (= 0 (count v)))
      (is (= [] v))))
  (testing "empty: defined"
    (let [config {:something {:projects []}}
          json-path-vector [:something :projects]
          v (common/get-all-scopes-from-collection-of-artifacts-projects config json-path-vector)]
      (is (vector? v))
      (is (= 0 (count v)))))
  (testing "one scope"
    (let [config {:something {:projects [{:scope "alpha"}]}}
          json-path-vector [:something :projects]
          v (common/get-all-scopes-from-collection-of-artifacts-projects config json-path-vector)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "alpha" (first v)))))
  (testing "two scopes"
    (let [config {:something {:projects [{:scope "alpha"} {:scope "bravo"}]}}
          json-path-vector [:something :projects]
          v (common/get-all-scopes-from-collection-of-artifacts-projects config json-path-vector)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (first v)))
      (is (= "bravo" (nth v 1)))))
  (testing "three scopes"
    (let [config {:something {:projects [{:scope "alpha"} {:scope "bravo"} {:scope "charlie"}]}}
          json-path-vector [:something :projects]
          v (common/get-all-scopes-from-collection-of-artifacts-projects config json-path-vector)]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "alpha" (first v)))
      (is (= "bravo" (nth v 1)))
      (is (= "charlie" (nth v 2))))))


(deftest get-full-scope-paths-test
  (testing "both empty"
    (let [scope-path-vector []
          scope-vector []
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 0 (count v)))
      (is (= [] v))))
  (testing "scope-path-vector empty"
    (let [scope-path-vector []
          scope-vector [:charlie]
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= [:charlie] (first v)))))
  (testing "scope-vector empty"
    (let [scope-path-vector [:alpha]
          scope-vector []
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 0 (count v)))))
  (testing "scope-path-vector has one entry, scope-vector has one entry"
    (let [scope-path-vector [:alpha]
          scope-vector [:charlie]
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= [[:alpha :charlie]] v))))
  (testing "scope-path-vector has two entries, scope-vector has one entry"
    (let [scope-path-vector [:alpha :bravo]
          scope-vector [:charlie]
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= [[:alpha :bravo :charlie]] v))))
  (testing "scope-path-vector has one entry, scope-vector has two entries"
    (let [scope-path-vector [:alpha]
          scope-vector [:charlie :delta]
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= [[:alpha :charlie] [:alpha :delta]] v))))
  (testing "scope-path-vector has two entries, scope-vector has two entries"
    (let [scope-path-vector [:alpha :bravo]
          scope-vector [:charlie :delta]
          v (common/get-full-scope-paths scope-path-vector scope-vector)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= [[:alpha :bravo :charlie] [:alpha :bravo :delta]] v)))))


(defn check-get-all-full-scopes-test
  [expected actual]
  (is (vector? actual))
  (is (= (count expected) (count actual)))
  (is (= expected actual)))


(deftest get-all-full-scopes-test
  (testing "no artifacts or projects"
    (let [config {:project {:scope "alpha"}}
          v (common/get-all-full-scopes config)]
      (is (vector? v))
      (is (= 1 (count v)))
      (check-get-all-full-scopes-test ["alpha"] (nth v 0))))
  (testing "1 artifact but no projects"
    (let [config {:project {:scope "alpha" :artifacts [{:scope "alpha-art-1"}]}}
          v (common/get-all-full-scopes config)]
      (is (vector? v))
      (is (= 2 (count v)))
      (check-get-all-full-scopes-test ["alpha"] (nth v 0))
      (check-get-all-full-scopes-test ["alpha" "alpha-art-1"] (nth v 1))))
  (testing "3 artifacts but no projects"
    (let [config {:project {:scope "alpha" :artifacts [{:scope "alpha-art-1"} {:scope "alpha-art-2"} {:scope "alpha-art-3"}]}}
          v (common/get-all-full-scopes config)]
      (is (vector? v))
      (is (= 4 (count v)))
      (check-get-all-full-scopes-test ["alpha"] (nth v 0))
      (check-get-all-full-scopes-test ["alpha" "alpha-art-1"] (nth v 1))
      (check-get-all-full-scopes-test ["alpha" "alpha-art-2"] (nth v 2))
      (check-get-all-full-scopes-test ["alpha" "alpha-art-3"] (nth v 3))))
  (testing "no artifacts but 1 project"
    (let [config {:project {:scope "alpha" :projects [{:scope "bravo"}]}}
          v (common/get-all-full-scopes config)]
      (is (vector? v))
      (is (= 2 (count v)))
      (check-get-all-full-scopes-test ["alpha"] (nth v 0))
      (check-get-all-full-scopes-test ["alpha" "bravo"] (nth v 1))))
  (testing "no artifacts but 3 projects"
    (let [config {:project {:scope "alpha" :projects [{:scope "bravo"} {:scope "charlie"} {:scope "delta"}]}}
          v (common/get-all-full-scopes config)]
      (is (vector? v))
      (is (= 4 (count v)))
      (check-get-all-full-scopes-test ["alpha"] (nth v 0))
      (check-get-all-full-scopes-test ["alpha" "bravo"] (nth v 1))
      (check-get-all-full-scopes-test ["alpha" "charlie"] (nth v 2))
      (check-get-all-full-scopes-test ["alpha" "delta"] (nth v 3))))
  (testing "numerous projects/sub-projects and artifacts"
    (let [config {:project {:scope "alpha"
                            :artifacts [{:scope "alpha-art-1"}
                                        {:scope "alpha-art-2"}
                                        {:scope "alpha-art-3"}]
                            :projects [{:scope "bravo"
                                        :artifacts [{:scope "bravo-art-1"}
                                                    {:scope "bravo-art-2"}
                                                    {:scope "bravo-art-3"}]
                                        :projects [{:scope "echo"
                                                    :artifacts [{:scope "echo-art-1"}
                                                                {:scope "echo-art-2"}
                                                                {:scope "echo-art-3"}]
                                                    :projects [{:scope "foxtrot"
                                                                :artifacts [{:scope "foxtrot-art-1"}]}]}]}
                                       {:scope "charlie"
                                        :artifacts [{:scope "charlie-art-1"}
                                                    {:scope "charlie-art-2"}
                                                    {:scope "charlie-art-3"}]}
                                       {:scope "delta"
                                        :artifacts [{:scope "delta-art-1"}
                                                    {:scope "delta-art-2"}
                                                    {:scope "delta-art-3"}]}]}}
          v (common/get-all-full-scopes config)]
      (is (vector? v))
      (check-get-all-full-scopes-test [["alpha"]
                                       ["alpha" "alpha-art-1"]
                                       ["alpha" "alpha-art-2"]
                                       ["alpha" "alpha-art-3"]
                                       ["alpha" "bravo"]
                                       ["alpha" "bravo" "bravo-art-1"]
                                       ["alpha" "bravo" "bravo-art-2"]
                                       ["alpha" "bravo" "bravo-art-3"]
                                       ["alpha" "bravo" "echo"]
                                       ["alpha" "bravo" "echo" "echo-art-1"]
                                       ["alpha" "bravo" "echo" "echo-art-2"]
                                       ["alpha" "bravo" "echo" "echo-art-3"]
                                       ["alpha" "bravo" "echo" "foxtrot"]
                                       ["alpha" "bravo" "echo" "foxtrot" "foxtrot-art-1"]
                                       ["alpha" "charlie"]
                                       ["alpha" "charlie" "charlie-art-1"]
                                       ["alpha" "charlie" "charlie-art-2"]
                                       ["alpha" "charlie" "charlie-art-3"]
                                       ["alpha" "delta"]
                                       ["alpha" "delta" "delta-art-1"]
                                       ["alpha" "delta" "delta-art-2"]
                                       ["alpha" "delta" "delta-art-3"]] v))))



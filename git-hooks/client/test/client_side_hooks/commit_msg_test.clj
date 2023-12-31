;; (c) Copyright 2023 KineticFire. All rights reserved.
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
;;	  Project site:  https://github.com/kineticfire-labs/git-conventional-commits-hooks


(ns client-side-hooks.commit-msg-test
  (:require [clojure.test       :refer [deftest is testing]]
            [babashka.classpath :as cp]))

(cp/add-classpath "./")
(require '[client-side-hooks.commit-msg :as cm])


(deftest add-up-test
  (testing "test with positive ints"
    (is (= 4 (cm/add-up 2 2)))
    (is (= 2 (cm/add-up 1 1))))
  (testing "test with negative ints"
    (is (= 2 (cm/add-up 4 -2)))
    (is (= 1 (cm/add-up 2 -1)))))


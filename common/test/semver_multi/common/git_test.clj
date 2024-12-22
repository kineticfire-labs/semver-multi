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


(ns semver-multi.common.git-test
  (:require [clojure.test            :refer [deftest is testing]]
            [babashka.classpath      :as cp]
            [babashka.process        :refer [shell]]
            [semver-multi.common.git :as git]))


(cp/add-classpath "./")



(deftest get-git-root-dir-test
  (testing "success: in a git repo"
    (with-redefs [shell (constantly {:out "/home/user/repos/semver-multi" :err nil})]
      (is (= "/home/user/repos/semver-multi" (git/get-git-root-dir)))))
  (testing "success: in a git repo and return value would have had a newline"
    (with-redefs [shell (constantly {:out "/home/user/repos/semver-multi\n" :err nil})]
      (is (= "/home/user/repos/semver-multi" (git/get-git-root-dir)))))
  (testing "fail: not in a git repo"
    (with-redefs [shell (constantly {:out nil :err "fatal: not a git repository (or any of the parent directories): .git"})]
      (is (nil? (git/get-git-root-dir))))))


(deftest get-git-branch-test
  (with-redefs [shell (fn [_ _] {:out "main"})]
    (is (= "main" (git/get-git-branch)))))

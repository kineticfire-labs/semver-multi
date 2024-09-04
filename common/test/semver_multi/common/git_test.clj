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


(ns semver-multi.common.git-test
  (:require [clojure.test            :refer [deftest is testing]]
            [clojure.string          :as str]
            [babashka.classpath      :as cp]
            [babashka.process        :refer [shell]]
            [clojure.java.io         :as io]
            [semver-multi.common.git :as git])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/git-test")

(def ^:const resources-test-dir-string "test/resources/git")
(def ^:const resources-test-data-dir-string (str resources-test-dir-string "/data"))


;; from https://clojuredocs.org/clojure.core/with-out-str#example-590664dde4b01f4add58fe9f
(defmacro with-out-str-data-map
  "Performs the form in the `body` and returns a map result with key 'result' set to the return value and 'str' set to the string output, if any.  Equivalent to 'with-out-str' but returns a map result."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r# ~@body]
         {:result r#
          :str    (str s#)}))))


(defn delete-dir
  "Deletes the file or directory `file`.  If `file` is a directory, then first recursively deletes all contents."
  [^File file]
  (when (.isDirectory file)
     (run! delete-dir (.listFiles file)))
   (io/delete-file file))


(defn create-temp-sub-dir
  "Creates a a temp sub-directory 'sub-dir' under the temp directory 'temp-dir-string'."
  [sub-dir]
  (let [temp-dir (File. (str temp-dir-string "/" sub-dir))]
    (.mkdirs temp-dir)))


(defn setup-temp-dir
  "Sets up the temporary directory for the tests in this file.  Creates the directory if it does not exists, recursively deleting the directory first if it does exist."
  []
  (let [ temp-dir (File. temp-dir-string)]
    (when (.exists temp-dir)
      (delete-dir temp-dir))
    (.mkdirs temp-dir)))


;; Configures temporary directory for tests
(setup-temp-dir)



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
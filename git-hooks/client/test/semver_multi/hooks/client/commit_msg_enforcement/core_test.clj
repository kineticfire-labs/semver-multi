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


;; KineticFire Labs: https://labs.kineticfire.com/
;;	   Project site: https://github.com/kineticfire-labs/semver-multi/


(ns semver-multi.hooks.client.commit-msg-enforcement.core-test
  (:require [clojure.test                                          :refer [deftest is testing]]
            [babashka.classpath                                    :as cp]
            [babashka.process                                      :refer [shell]]
            [clojure.java.io                                       :as io]
            [semver-multi.hooks.client.commit-msg-enforcement.core :as cm]
            [semver-multi.common.system                            :as system])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/commit-msg-enforcement")

(def ^:const resources-test-dir-string "test/resources/commit-msg-enforcement")



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
  (let [temp-dir (File. temp-dir-string)]
    (when (.exists temp-dir)
      (delete-dir temp-dir))
    (.mkdirs temp-dir)))


;; Configures temporary directory for tests
(setup-temp-dir)


(defn copy-file
  "Copies the file identified by path string `source-path-string` to destination file identified by the path string `dest-path-string`."
  [source-path-string dest-path-string]
  (io/copy (io/file source-path-string) (io/file dest-path-string)))


;; todo put 'shell' redef in top-level
(deftest perform-check-test
  (with-redefs [system/exit-now! (fn [x] x)] 
    (let [local-resources-test-dir-string-slash (str resources-test-dir-string "/perform-check/")
          local-test-sub-dir "perform-check"
          local-test-dir-string-slash (str temp-dir-string "/" local-test-sub-dir "/")]
      (create-temp-sub-dir local-test-sub-dir)
      
      ;; args
      (testing "args: is empty"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [] (str local-resources-test-dir-string-slash "project-small.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error: exactly one argument required.  Usage:  commit-msg <path to git edit message>\\033[0m\\e[0m\"\n" (:str v))))))
      (testing "args: has two values"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check ["a" "b"] (str local-resources-test-dir-string-slash "project-small.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error: exactly one argument required.  Usage:  commit-msg <path to git edit message>\\033[0m\\e[0m\"\n" (:str v))))))
      
      ;; config file
      (testing "config file: can't open file"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_good-one-line")] (str local-resources-test-dir-string-slash "doesnt-exist.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error reading config file. File 'test/resources/commit-msg-enforcement/perform-check/doesnt-exist.json' not found. test/resources/commit-msg-enforcement/perform-check/doesnt-exist.json (No such file or directory)\\033[0m\\e[0m\"\n" (:str v))))))
      (testing "config file: parse fails"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_good-one-line")] (str local-resources-test-dir-string-slash "project-parse-fail.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error reading config file. JSON parse error when reading file 'test/resources/commit-msg-enforcement/perform-check/project-parse-fail.def.json'.\\033[0m\\e[0m\"\n" (:str v))))))
      (testing "config file: invalid format"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_good-one-line")] (str local-resources-test-dir-string-slash "project-invalid.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error validating config file at test/resources/commit-msg-enforcement/perform-check/project-invalid.def.json. Project required property 'scope' at property 'name' of 'simple-lib' and path '[:config :project]' must be a string.\\033[0m\\e[0m\"\n" (:str v))))))
      (testing "config file: disabled"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_good-one-line")] (str local-resources-test-dir-string-slash "project-disabled.def.json")))]
            (is (= 0 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[33mCOMMIT WARNING by local commit-msg hook.\"\necho -e \"\\e[1m\\e[33mCommit proceeding with warning: Commit message enforcement disabled.\\033[0m\\e[0m\"\n" (:str v))))))
      
      ;; commit message
      (testing "commit message: can't open file"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_doesnt-exist")] (str local-resources-test-dir-string-slash "project-large.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error reading git commit edit message file 'test/resources/commit-msg-enforcement/perform-check/COMMIT_EDIT_MSG_doesnt-exist'. File 'test/resources/commit-msg-enforcement/perform-check/COMMIT_EDIT_MSG_doesnt-exist' not found. test/resources/commit-msg-enforcement/perform-check/COMMIT_EDIT_MSG_doesnt-exist (No such file or directory)\\033[0m\\e[0m\"\n" (:str v))))))
      (testing "commit message: invalid format - line length of title line"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_bad-format")] (str local-resources-test-dir-string-slash "project-large.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Commit message invalid 'test/resources/commit-msg-enforcement/perform-check/COMMIT_EDIT_MSG_bad-format'. Commit message title line must not contain more than 50 characters.\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (1) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e \\e[1m\\e[31mfeat(p.client.app)!: add super neat feature but cause a commit message reject by adding a title line description that is too long\\033[0m\\e[0m\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
      (testing "commit message: invalid format - scope/type"
        (with-redefs [shell (fn [x] (println x))]
          (let [v (with-out-str-data-map (cm/perform-check [(str local-resources-test-dir-string-slash "COMMIT_EDIT_MSG_bad-scope-type")] (str local-resources-test-dir-string-slash "project-large.def.json")))]
            (is (= 1 (:result v)))
            (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Commit message invalid 'test/resources/commit-msg-enforcement/perform-check/COMMIT_EDIT_MSG_bad-scope-type'. Definition in title line of type 'zulu' for scope 'p.client.app' at query path of '[:project :projects 0 :artifacts 0]' not found in config.\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (1) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e \\e[1m\\e[31mzulu(p.client.app)!: add super neat feature\\033[0m\\e[0m\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
      
      ;; note: did not test commit message write failure
      
      ;; success
      (testing "success: one-line commit message reformatted"
        (with-redefs [shell (fn [x] (println x))]
          (let [file-string "COMMIT_EDIT_MSG_good-one-line"
                from-file-path-string (str local-resources-test-dir-string-slash file-string)
                to-file-path-string (str local-test-dir-string-slash file-string)]
            (copy-file from-file-path-string to-file-path-string)
            (let [v (with-out-str-data-map (cm/perform-check [to-file-path-string] (str local-resources-test-dir-string-slash "project-large.def.json")))]
              (is (= 0 (:result v)))
              (is (= "echo -e \"\\e[0m\\e[1mCommit ok, per by local commit-msg hook.\"\n" (:str v)))
              (is (= "feat(p.client.app)!: add super neat feature" (slurp to-file-path-string)))))))
      (testing "success: multi-line commit message reformatted"
        (with-redefs [shell (fn [x] (println x))]
          (let [file-string "COMMIT_EDIT_MSG_good-multi-line"
                from-file-path-string (str local-resources-test-dir-string-slash file-string)
                to-file-path-string (str local-test-dir-string-slash file-string)]
            (copy-file from-file-path-string to-file-path-string)
            (let [v (with-out-str-data-map (cm/perform-check [to-file-path-string] (str local-resources-test-dir-string-slash "project-large.def.json")))]
              (is (= 0 (:result v)))
              (is (= "echo -e \"\\e[0m\\e[1mCommit ok, per by local commit-msg hook.\"\n" (:str v)))
              (is (= "feat(p.client.app)!: add super neat feature\n\nSupport new data with addition of super neat feature\n\nAnother line\nDirectly after line\n\nAnother line\n\n     This line has 5 spaces before, which is ok\n\nThis line has 5 spaces after this\n\nLine with 4 spaces only below\n\nLast real line\n\nBREAKING CHANGE: a big change\nBREAKING CHANGE: a big change\nBREAKING CHANGE: a big change\n\nBREAKING CHANGE: a big change\nBREAKING CHANGE: a big change\n\nBREAKING CHANGE: a big change\nBREAKING CHANGE: a big change\nBREAKING CHANGE: a big change\n\nBREAKING CHANGE: a big change" (slurp to-file-path-string))))))))))

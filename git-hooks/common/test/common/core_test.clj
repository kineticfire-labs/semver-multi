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

(ns common.core-test
  (:require [clojure.test       :refer [deftest is testing]]
            [clojure.string     :as str]
            [babashka.classpath :as cp]
            [babashka.process   :refer [shell]]
            [clojure.java.io    :as io]
            [common.core        :as common])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/core-test")

(def ^:const resources-test-data-dir-string "test/resources/data")


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


(defn setup-temp-dir
  "Sets up the temporary directory for the tests in this file.  Creates the directory if it does not exists, recursively deleting the directory first if it does exist."
  []
  (let [ temp-dir (File. temp-dir-string)]
    (when (.exists temp-dir)
      (delete-dir temp-dir))
    (.mkdirs temp-dir)))


;; Configures temporary directory for tests
(setup-temp-dir)


(deftest do-on-success-test
  (testing "one arg: success"
    (let [v (common/do-on-success #(update % :val inc) {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 2 (:val v)))))
  (testing "one arg: fail"
    (let [v (common/do-on-success #(update % :val inc) {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "two args: success"
    (let [v (common/do-on-success #(assoc %2 :val (+ (:val %2) %1)) 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 6 (:val v)))))
  (testing "two args: fail"
    (let [v (common/do-on-success #(assoc %2 :val (+ (:val %2) %1)) 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "three args: success"
    (let [v (common/do-on-success #(assoc %3 :val (+ (:val %3) %1 %2)) 2 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 8 (:val v)))))
  (testing "three args: fail"
    (let [v (common/do-on-success #(assoc %3 :val (+ (:val %3) %1 %2)) 2 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "four args: success"
    (let [v (common/do-on-success #(assoc %4 :val (+ (:val %4) %1 %2 %3)) 1 2 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 9 (:val v)))))
  (testing "four args: fail"
    (let [v (common/do-on-success #(assoc %4 :val (+ (:val %4) %1 %2 %3)) 1 2 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v))))))


(deftest get-git-root-dir-test
  (testing "success: in a git repo"
    (with-redefs [shell (constantly {:out "/home/user/repos/semver-multi" :err nil})]
      (is (= "/home/user/repos/semver-multi" (common/get-git-root-dir)))))
  (testing "success: in a git repo and return value would have had a newline"
    (with-redefs [shell (constantly {:out "/home/user/repos/semver-multi\n" :err nil})]
      (is (= "/home/user/repos/semver-multi" (common/get-git-root-dir)))))
  (testing "fail: not in a git repo"
    (with-redefs [shell (constantly {:out nil :err "fatal: not a git repository (or any of the parent directories): .git"})]
      (is (nil? (common/get-git-root-dir))))))


(deftest get-git-branch-test
  (with-redefs [shell (fn [_ _] {:out "main"})]
    (is (= "main" (common/get-git-branch)))))


(deftest split-lines-test
  (testing "empty string"
    (let [v (common/split-lines "")]
      (is (= 1 (count v)))
      (is (= "" (first v)))
      (is (vector? v))))
  (testing "single line"
    (let [v (common/split-lines "One long line")]
      (is (= 1 (count v)))
      (is (= "One long line" (first v)))
      (is (vector? v))))
  (testing "multiple lines"
    (let [v (common/split-lines "First line\nSecond line\nThird line")]
      (is (= 3 (count v)))
      (is (= "First line" (first v)))
      (is (= "Second line" (nth v 1)))
      (is (= "Third line" (nth v 2)))
      (is (vector? v)))))


(deftest run-shell-command-test
  (testing "string, empty"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "\n" (with-out-str (common/run-shell-command ""))))))
  (testing "string, non-empty"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "item1\n" (with-out-str (common/run-shell-command "item1"))))))
  (testing "vector, empty"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "" (with-out-str (common/run-shell-command []))))))
  (testing "vector, empty string"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "\n" (with-out-str (common/run-shell-command [""]))))))
  (testing "vector, one string"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "item1\n" (with-out-str (common/run-shell-command ["item1"]))))))
  (testing "vector, two strings"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "item1\nitem2\n" (with-out-str (common/run-shell-command ["item1" "item2"])))))))


(deftest apply-display-with-shell-test
  (testing "string input"
    (let [v (common/apply-display-with-shell "test line")]
      (is (string? v))
      (is (= "echo -e test line" v))))
  (testing "vector string input"
    (let [v (common/apply-display-with-shell ["test line 1" "test line 2" "test line 3"])]
      (is (seq? v))
      (is (= 3 (count v)))
      (is (= "echo -e test line 1" (nth v 0)))
      (is (= "echo -e test line 2" (nth v 1)))
      (is (= "echo -e test line 3" (nth v 2))))))


(deftest apply-display-with-shell-without-newline-test
 (testing "string input"
   (let [v (common/apply-display-with-shell-without-newline "test line")]
     (is (string? v))
     (is (= "echo -n -e test line" v)))))


(deftest apply-quotes
  (testing "string input"
    (let [v (common/apply-quotes "test line")]
      (is (string? v))
      (is (= "\"test line\"" v))))
  (testing "vector string input"
    (let [v (common/apply-quotes ["test line 1" "test line 2" "test line 3"])]
      (is (seq? v))
      (is (= 3 (count v)))
      (is (= "\"test line 1\"" (nth v 0)))
      (is (= "\"test line 2\"" (nth v 1)))
      (is (= "\"test line 3\"" (nth v 2))))))


(deftest generate-shell-newline-characters-test
  (testing "no arg"
    (let [v (common/generate-shell-newline-characters)]
      (is (string? v))
      (is (= "\n" v))))
  (testing "arg=1"
    (let [v (common/generate-shell-newline-characters 1)]
      (is (string? v))
      (is (= "\n" v))))
  (testing "arg=3"
    (let [v (common/generate-shell-newline-characters 3)]
      (is (string? v))
      (is (= "\n\n\n" v)))))


(deftest generate-commit-msg-offending-line-header-test
  (testing "lines is empty vector"
    (let [v (common/generate-commit-msg-offending-line-header [] nil)]
      (is (vector? v))
      (is (= 0 (count v)))))
  (testing "lines is empty string"
    (let [v (common/generate-commit-msg-offending-line-header [""] nil)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "" (first v)))))
  (testing "lines-num is nil (no offending line)"
    (let [v (common/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] nil)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num is empty sequence (no offending line)"
    (let [v (common/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] '())]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num indicates first line"
    (let [v (common/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] '(0))]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))
      (is (= "\"   offending line(s) # (1) in red **************\"" (nth v 2)))))
  (testing "lines-num indicates second line"
    (let [v (common/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] '(1))]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))
      (is (= "\"   offending line(s) # (2) in red **************\"" (nth v 2)))))
  (testing "lines-num indicates first and third lines, but not second line"
    (let [v (common/generate-commit-msg-offending-line-header ["Line 1" "Line 2" "Line 3"] '(0 2))]
      (is (vector? v))
      (is (= 4 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))
      (is (= "Line 3" (nth v 2)))
      (is (= "\"   offending line(s) # (1 3) in red **************\"" (nth v 3))))))


(deftest generate-commit-msg-offending-line-msg-highlight-test
  (testing "lines is empty vector"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight [] nil)]
      (is (vector? v))
      (is (= 0 (count v)))))
  (testing "lines is empty string"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight [""] nil)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "" (first v)))))
  (testing "lines-num is nil (no offending line)"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] nil)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num is empty (no offending line)"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] '())]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num indicates first line"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] '(0))]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "\\e[1m\\e[31mLine 1\\033[0m\\e[0m" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num indicates second line"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] '(1))]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "\\e[1m\\e[31mLine 2\\033[0m\\e[0m" (nth v 1)))))
  (testing "lines-num indicates first and third lines"
    (let [v (common/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2" "Line 3"] '(0 2))]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "\\e[1m\\e[31mLine 1\\033[0m\\e[0m" (nth v 0)))
      (is (= "Line 2" (nth v 1)))
      (is (= "\\e[1m\\e[31mLine 3\\033[0m\\e[0m" (nth v 2))))))


(deftest generate-commit-msg-test
  (testing "lines is empty string"
    (let [v (common/generate-commit-msg "")]
      (is (seq? v))
      (is (= 7 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (= "echo -e " (nth v 3)))
      (is (true? (str/includes? (nth v 5) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num is nil (no offending line)"
    (let [v (common/generate-commit-msg "Line 1\nLine 2" nil)]
      (is (seq? v))
      (is (= 8 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (= "echo -e Line 1" (nth v 3)))
      (is (= "echo -e Line 2" (nth v 4)))
      (is (true? (str/includes? (nth v 6) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num is empty (no offending line)"
    (let [v (common/generate-commit-msg "Line 1\nLine 2" '())]
      (is (seq? v))
      (is (= 8 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (= "echo -e Line 1" (nth v 3)))
      (is (= "echo -e Line 2" (nth v 4)))
      (is (true? (str/includes? (nth v 6) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num indicates first line"
    (let [v (common/generate-commit-msg "Line 1\nLine 2" '(0))]
      (is (seq? v))
      (is (= 9 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (true? (str/includes? (nth v 2) "echo -e \"   offending line(s) # (1) in red")))
      (is (= "echo -e \\e[1m\\e[31mLine 1\\033[0m\\e[0m" (nth v 4))) 
      (is (= "echo -e Line 2" (nth v 5)))
      (is (true? (str/includes? (nth v 7) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num indicates second line"
    (let [v (common/generate-commit-msg "Line 1\nLine 2" '(1))]
      (is (seq? v))
      (is (= 9 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (true? (str/includes? (nth v 2) "echo -e \"   offending line(s) # (2) in red")))
      (is (= "echo -e Line 1" (nth v 4)))
      (is (= "echo -e \\e[1m\\e[31mLine 2\\033[0m\\e[0m" (nth v 5)))
      (is (true? (str/includes? (nth v 7) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num indicates first and third lines"
    (let [v (common/generate-commit-msg "Line 1\nLine 2\nLine 3" '(0 2))]
      (is (seq? v))
      (is (= 10 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (true? (str/includes? (nth v 2) "echo -e \"   offending line(s) # (1 3) in red")))
      (is (= "echo -e \\e[1m\\e[31mLine 1\\033[0m\\e[0m" (nth v 4)))
      (is (= "echo -e Line 2" (nth v 5)))
      (is (= "echo -e \\e[1m\\e[31mLine 3\\033[0m\\e[0m" (nth v 6)))
      (is (true? (str/includes? (nth v 8) "echo -e \"END - COMMIT MESSAGE"))))))


(deftest generate-commit-err-msg-test
  (testing "title and err-msg"
    (let [v (common/generate-commit-err-msg "A title." "An error message.")]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED A title.\"" (first v)))
      (is (= "echo -e \"\\e[1m\\e[31mCommit failed reason: An error message.\\033[0m\\e[0m\"" (nth v 1))))))


(deftest handle-err-test
  (with-redefs [common/exit-now! (fn [x] x)]
    (testing "title and error msg"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-err "The title" "The err message"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and one-line commit msg w/o line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-err "The title" "The err message" "Commit msg line 1"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e Commit msg line 1\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and multi-line commit msg w/o line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-err "The title" "The err message" "Commit msg line 1\nAnd line 2\nAnd line 3"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e Commit msg line 1\necho -e And line 2\necho -e And line 3\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and one-line commit msg w/ line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-err "The title" "The err message" "Commit msg line 1" [0]))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (1) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e \\e[1m\\e[31mCommit msg line 1\\033[0m\\e[0m\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and multi-line commit msg w/ line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-err "The title" "The err message" "Commit msg line 1\nAnd line 2\nAnd line 3" [1]))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (2) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e Commit msg line 1\necho -e \\e[1m\\e[31mAnd line 2\\033[0m\\e[0m\necho -e And line 3\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))))


(deftest generate-commit-warn-msg-test
  (testing "title and err-msg"
    (let [v (common/generate-commit-warn-msg "A title." "A warning message.")]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (= "echo -e \"\\e[1m\\e[33mCOMMIT WARNING A title.\"" (first v)))
      (is (= "echo -e \"\\e[1m\\e[33mCommit proceeding with warning: A warning message.\\033[0m\\e[0m\"" (nth v 1))))))


(deftest handle-warn-proceed-test
  (with-redefs [common/exit-now! (fn [x] x)]
    (testing "normal usage"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-warn-proceed "The title" "The message"))]
          (is (= 0 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[33mCOMMIT WARNING The title\"\necho -e \"\\e[1m\\e[33mCommit proceeding with warning: The message\\033[0m\\e[0m\"\n" (:str v))))))))


(deftest handle-ok
  (with-redefs [common/exit-now! (fn [x] x)]
    (testing "normal usage"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (common/handle-ok "The title"))]
          (is (= 0 (:result v)))
          (is (= "echo -e \"\\e[0m\\e[1mCommit ok, per The title\"\n" (:str v))))))))


(deftest parse-json-file-test
  (testing "file not found"
    (let [v (common/parse-json-file (str resources-test-data-dir-string "/" "does-not-exist.json"))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (str/includes? (:reason v) "File 'test/resources/data/does-not-exist.json' not found.")))))
  (testing "parse fail"
    (let [v (common/parse-json-file (str resources-test-data-dir-string "/" "parse-bad.json"))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (str/includes? (:reason v) "JSON parse error when reading file 'test/resources/data/parse-bad.json'.")))))
  (testing "parse ok"
    (let [v (common/parse-json-file (str resources-test-data-dir-string "/" "parse-good.json"))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (map? (:result v)))
      (is (= "hi" (:cb (:c (:result v))))))))


(defn perform-is-semantic-version-release?
  [version result]
  (let [v (common/is-semantic-version-release? version)]
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


(deftest validate-config-fail-test
  (testing "msg only"
    (let [v (common/validate-config-fail "An error message.")]
      (is (map? v))
      (is (string? (:reason v)))
      (is (= "An error message." (:reason v)))
      (is (boolean? (:success v)))
      (is (false? (:success v)))))
  (testing "map and msg"
    (let [v (common/validate-config-fail "An error message." {:other "abcd"})]
      (is (map? v))
      (is (string? (:reason v)))
      (is (= "An error message." (:reason v)))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:other v)))
      (is (= "abcd" (:other v))))))


(deftest validate-config-param-string-test 
  (testing "required, present, valid, not empty string, empty string ok"
    (let [v (common/validate-config-param-string {:a {:b "alpha"}} [:a :b] true true)]
      (is (boolean? v))
      (is (true? v))))
  (testing "required, present, valid, empty string, empty string not ok"
    (let [v (common/validate-config-param-string {:a {:b ""}} [:a :b] true false)]
      (is (boolean? v))
      (is (false? v))))
  (testing "required, present, invalid"
    (let [v (common/validate-config-param-string {:a {:b 1}} [:a :b] true false)]
      (is (boolean? v))
      (is (false? v))))
  (testing "required, not present"
    (let [v (common/validate-config-param-string {:a {:c "bravo"}} [:a :b] true true)]
      (is (boolean? v))
      (is (false? v))))
  (testing "optional, present, valid, not empty string"
    (let [v (common/validate-config-param-string {:a {:b "alpha"}} [:a :b] false true)]
      (is (boolean? v))
      (is (true? v))))
  (testing "optional, present, valid, empty string, empty string ok"
    (let [v (common/validate-config-param-string {:a {:b ""}} [:a :b] false true)]
      (is (boolean? v))
      (is (true? v))))
  (testing "optional, present, valid, empty string, empty string not ok"
    (let [v (common/validate-config-param-string {:a {:b ""}} [:a :b] false false)]
      (is (boolean? v))
      (is (false? v))))
  (testing "optional, present, invalid"
    (let [v (common/validate-config-param-string {:a {:b 1}} [:a :b] false false)]
      (is (boolean? v))
      (is (false? v))))
  (testing "optional, not present"
    (let [v (common/validate-config-param-string {:a {:c "bravo"}} [:a :b] false true)]
      (is (boolean? v))
      (is (true? v)))))


(deftest validate-config-param-array-test
 (testing "required, present, 1 element, fn valid"
   (let [v (common/validate-config-param-array {:a {:b ["alpha"]}} [:a :b] true string?)]
     (is (boolean? v))
     (is (true? v))))
  (testing "required, present, 2 elements, fn valid"
    (let [v (common/validate-config-param-array {:a {:b ["alpha" "bravo"]}} [:a :b] true string?)]
      (is (boolean? v))
      (is (true? v))))
  (testing "required, present, fn not valid"
    (let [v (common/validate-config-param-array {:a {:b ["alpha" 1]}} [:a :b] true string?)]
      (is (boolean? v))
      (is (false? v))))
  (testing "required, not present"
    (let [v (common/validate-config-param-array {:a {:c ["alpha"]}} [:a :b] true string?)]
      (is (boolean? v))
      (is (false? v))))
  (testing "optional, present, fn valid"
    (let [v (common/validate-config-param-array {:a {:b ["alpha"]}} [:a :b] false string?)]
      (is (boolean? v))
      (is (true? v))))
 (testing "optional, not present, fn valid"
   (let [v (common/validate-config-param-array {:a {:c ["alpha"]}} [:a :b] false string?)]
     (is (boolean? v))
     (is (true? v))))
  (testing "optional, present, fn invalid"
    (let [v (common/validate-config-param-array {:a {:b ["alpha" 1]}} [:a :b] false string?)]
      (is (boolean? v))
      (is (false? v)))))


(deftest validate-config-msg-enforcement-test
  (testing "enforcement block not defined"
    (let [v (common/validate-config-msg-enforcement {:config {}})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= "Commit message enforcement block (commit-msg-enforcement) must be defined." (:reason v)))
      (is (true? (contains? v :config)))))
  (testing "'enabled' not defined"
    (let [v (common/validate-config-msg-enforcement {:config {:commit-msg-enforcement {}}})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= "Commit message enforcement must be set as enabled or disabled (commit-msg-enforcement.enabled) with either 'true' or 'false'." (:reason v)))
      (is (true? (contains? v :config)))))
  (testing "'enabled' set to nil"
    (let [v (common/validate-config-msg-enforcement {:config {:commit-msg-enforcement {:enabled nil}}})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= "Commit message enforcement must be set as enabled or disabled (commit-msg-enforcement.enabled) with either 'true' or 'false'." (:reason v)))
      (is (true? (contains? v :config)))))
  (testing "'enabled' set to true"
    (let [v (common/validate-config-msg-enforcement {:config {:commit-msg-enforcement {:enabled true}}})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (false? (contains? v :reason)))
      (is (true? (contains? v :config)))))
  (testing "'enabled' set to false"
    (let [v (common/validate-config-msg-enforcement {:config {:commit-msg-enforcement {:enabled false}}})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (false? (contains? v :reason)))
      (is (true? (contains? v :config))))))


(deftest validate-config-commit-msg-length-test
  ;; keys are defined
  (testing "title-line.min is not defined"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:max 20}
                                                                           :body-line {:min 2
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of title line (length.title-line.min) must be defined.")))))
  (testing "title-line.max is not defined"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12}
                                                                           :body-line {:min 2
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of title line (length.title-line.max) must be defined.")))))
  (testing "body-line.min is not defined"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of body line (length.body-line.min) must be defined.")))))
  (testing "body-line.max is not defined"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:min 2}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of body line (length.body-line.max) must be defined.")))))
  ;; title-line min/max and relative
  (testing "title-line.min is negative"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min -1
                                                                                        :max 20}
                                                                           :body-line {:min 2
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of title line (length.title-line.min) must be a positive integer.")))))
  (testing "title-line.min is zero"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 0
                                                                                        :max 20}
                                                                           :body-line {:min 2
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of title line (length.title-line.min) must be a positive integer.")))))
  (testing "title-line.max is negative"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max -1}
                                                                           :body-line {:min 2
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of title line (length.title-line.max) must be a positive integer.")))))
  (testing "title-line.max is zero"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 0}
                                                                           :body-line {:min 2
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of title line (length.title-line.max) must be a positive integer.")))))
  (testing "title-line.max is less than title-line.min"
     (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                         :max 11}
                                                                            :body-line {:min 2
                                                                                        :max 10}}}}})]
       (is (map? v))
       (is (boolean? (:success v)))
       (is (false? (:success v)))
       (is (string? (:reason v)))
       (is (true? (= (:reason v) "Maximum length of title line (length.title-line.max) must be equal to or greater than minimum length of title line (length.title-line.min).")))))
  ;; body-line min/max and relative)
  (testing "body-line.min is negative"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:min -1
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of body line (length.body-line.min) must be a positive integer.")))))
  (testing "body-line.min is zero"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:min 0
                                                                                       :max 10}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of body line (length.body-line.min) must be a positive integer.")))))
  (testing "body-line.max is negative"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:min 2
                                                                                       :max -1}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of body line (length.body-line.max) must be a positive integer.")))))
  (testing "body-line.max is zero"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:min 2
                                                                                       :max 0}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of body line (length.body-line.max) must be a positive integer.")))))
  (testing "title-line.max is less than title-line.min"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                        :max 20}
                                                                           :body-line {:min 2
                                                                                       :max 1}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of body line (length.body-line.max) must be equal to or greater than minimum length of body line (length.body-line.min).")))))
  (testing "success"
    (let [v (common/validate-config-commit-msg-length {:config {:commit-msg {:length {:title-line {:min 12
                                                                                                   :max 20}
                                                                                      :body-line {:min 2
                                                                                                  :max 30}}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v))))))


;; todo: validate-config-release-branches
(deftest validate-config-release-branches-test
 (testing "valid, 1 entry"
   (let [v (common/validate-config-release-branches {:config {:release-branches ["alpha"]}})]
     (is (map? v))
     (is (true? (contains? v :config)))
     (is (true? (:success v))))))


(deftest validate-config-for-root-project-test
  (testing "project valid"
    (let [v (common/validate-config-for-root-project {:config {:project {:a 1 :b 2}}})]
      (is (map? v))
      (is (true? (contains? v :config)))
      (is (true? (:success v)))))
  (testing "project invalid: property not defined"
    (let [v (common/validate-config-for-root-project {:config {:another {:a 1 :b 2}}})]
      (is (map? v))
      (is (true? (contains? v :config)))
      (is (false? (:success v)))
      (is (= (:reason v) "Property 'project' must be defined at the top-level."))))
  (testing "project invalid: project is nil"
    (let [v (common/validate-config-for-root-project {:config {:project nil}})]
      (is (map? v))
      (is (true? (contains? v :config)))
      (is (false? (:success v)))
      (is (= (:reason v) "Property 'project' must be defined at the top-level."))))
  (testing "project invalid: property is a scalar vs a map"
    (let [v (common/validate-config-for-root-project {:config {:project 5}})]
      (is (map? v))
      (is (true? (contains? v :config)))
      (is (false? (:success v)))
      (is (= (:reason v) "Property 'project' must be a map."))))
  (testing "project invalid: property is a vector vs a map"
    (let [v (common/validate-config-for-root-project {:config {:project [5]}})]
      (is (map? v))
      (is (true? (contains? v :config)))
      (is (false? (:success v)))
      (is (= (:reason v) "Property 'project' must be a map.")))))


(deftest validate-config-get-depends-on-test
  (testing "depends-on not defined"
    (let [v (common/validate-config-get-depends-on nil "a.b")]
      (is (vector? v))
      (is (= (count v) 0))))
  (testing "depends-on empty"
    (let [v (common/validate-config-get-depends-on [] "a.b")]
      (is (vector? v))
      (is (= (count v) 0))))
  (testing "depends-on has one item"
    (let [v (common/validate-config-get-depends-on ["alpha"] "a.b")]
      (is (vector? v))
      (is (= (count v) 1))
      (is (= v [["alpha" "a.b"]]))))
  (testing "depends-on has two items"
    (let [v (common/validate-config-get-depends-on ["alpha" "bravo"] "a.b")]
      (is (vector? v))
      (is (= (count v) 2))
      (is (= v [["alpha" "a.b"] ["bravo" "a.b"]])))))


(deftest validate-config-project-artifact-common-test
  (testing "valid config with all optional properties"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :projects [{:name "Subproject A"
                                                                                                                        :description "The subproject A"
                                                                                                                        :scope "proja"
                                                                                                                        :scope-alias "a"
                                                                                                                        :types ["feat", "chore", "refactor"]}
                                                                                                                       {:name "Subproject B"
                                                                                                                        :description "The subproject B"
                                                                                                                        :scope "projb"
                                                                                                                        :scope-alias "b"
                                                                                                                        :types ["feat", "chore", "refactor"]}]
                                                                                                            :artifacts [{:name "Artifact Y"
                                                                                                                         :description "The artifact Y"
                                                                                                                         :scope "arty"
                                                                                                                         :scope-alias "y"
                                                                                                                         :types ["feat", "chore", "refactor"]}
                                                                                                                        {:name "Artifact Z"
                                                                                                                         :description "The artifact Z"
                                                                                                                         :scope "artz"
                                                                                                                         :scope-alias "z"
                                                                                                                         :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "valid config without optional properties but with 'projects' and 'artifacts"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :scope "proj"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :projects [{:name "Subproject A"
                                                                                                                        :scope "proja"
                                                                                                                        :types ["feat", "chore", "refactor"]}
                                                                                                                       {:name "Subproject B"
                                                                                                                        :scope "projb"
                                                                                                                        :types ["feat", "chore", "refactor"]}]
                                                                                                            :artifacts [{:name "Artifact Y"
                                                                                                                         :scope "arty"
                                                                                                                         :types ["feat", "chore", "refactor"]}
                                                                                                                        {:name "Artifact Z"
                                                                                                                         :scope "artz"
                                                                                                                         :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "valid config without optional properties and without 'projects'; use 'artifact' node-type"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :scope "proj"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "valid config without optional properties and without 'projects'"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :scope "proj"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "invalid config: name not defined; use 'artifact' node-type"
    (let [v (common/validate-config-project-artifact-common :artifact [:config :project] {:config {:project {:description "The top project"
                                                                                                             :scope "proj"
                                                                                                             :scope-alias "p"
                                                                                                             :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'name' at path '[:config :project]' must be a string."))))
  (testing "invalid config: name not defined"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project required property 'name' at path '[:config :project]' must be a string."))))
  (testing "invalid config: name not a string"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name 5
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project required property 'name' at path '[:config :project]' must be a string."))))
  (testing "invalid config: description not a string"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description 5
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project optional property 'description' at property 'name' of 'Top Project' and path '[:config :project]' must be a string."))))
  (testing "invalid config: scope not defined"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project required property 'scope' at property 'name' of 'Top Project' and path '[:config :project]' must be a string."))))
  (testing "invalid config: scope not a string"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope 5
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project required property 'scope' at property 'name' of 'Top Project' and path '[:config :project]' must be a string."))))
  (testing "invalid config: scope-alias not a string"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias 5
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project optional property 'scope-alias' at property 'name' of 'Top Project' and path '[:config :project]' must be a string."))))
  (testing "invalid config: types not defined"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project required property 'types' at property 'name' of 'Top Project' and path '[:config :project]' must be an array of strings."))))
  (testing "invalid config: types not an array"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types {:object-invalid 5}}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project required property 'types' at property 'name' of 'Top Project' and path '[:config :project]' must be an array of strings."))))
  (testing "invalid config: depends-on not an array"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :depends-on {:object-invalid 5}}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project optional property 'depends-on' at property 'name' of 'Top Project' and path '[:config :project]' must be an array of strings."))))
  (testing "invalid config: can't define property 'project' on non-root project"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :project {:name "Invalid Project"}
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project cannot have property 'project' at property 'name' of 'Top Project' and path '[:config :project]'."))))
  (testing "valid config"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :depends-on ["proj.client"]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "depends-on not defined"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (count (:depends-on v)) 0))))
  (testing "depends-on empty"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :depends-on []}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project optional property 'depends-on' at property 'name' of 'Top Project' and path '[:config :project]' must be an array of strings."))))
  (testing "depends-on has one item"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :depends-on ["proj.client"]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (count (:depends-on v)) 1))
      (is (= (:depends-on v) [["proj.client" [:config :project]]]))))
  (testing "depends-on has two items"
    (let [v (common/validate-config-project-artifact-common :project [:config :project] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :depends-on ["proj.client" "proj.server"]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (count (:depends-on v)) 2))
      (is (= (:depends-on v) [["proj.client" [:config :project]] ["proj.server" [:config :project]]]))))
  (testing "with artifact, depends-on has two items"
    (let [v (common/validate-config-project-artifact-common :project [:config :project :artifacts 0] {:config {:project {:name "Top Project"
                                                                                                                         :description "The top project"
                                                                                                                         :scope "proj"
                                                                                                                         :scope-alias "p"
                                                                                                                         :types ["feat", "chore", "refactor"]
                                                                                                                         :depends-on ["proj.client" "proj.server"]
                                                                                                                         :artifacts [{:name "Artifact 1"
                                                                                                                                      :description "Artifact 1"
                                                                                                                                      :scope "art1"
                                                                                                                                      :scope-alias "a1"
                                                                                                                                      :types ["feat", "chore", "refactor"]
                                                                                                                                      :depends-on ["top-z" "top-y"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (count (:depends-on v)) 2))
      (is (= (:depends-on v) [["top-z" [:config :project :artifacts 0]] ["top-y" [:config :project :artifacts 0]]]))))
  (testing "with artifact, depends-on has two items.  'data' has 'depends-on' with two items."
    (let [v (common/validate-config-project-artifact-common :project [:config :project :artifacts 0] {:depends-on [["alpha" [:proj :alpha]] ["bravo" [:proj :bravo]]]
                                                                                                      :config {:project {:name "Top Project"
                                                                                                                         :description "The top project"
                                                                                                                         :scope "proj"
                                                                                                                         :scope-alias "p"
                                                                                                                         :types ["feat", "chore", "refactor"]
                                                                                                                         :depends-on ["proj.client" "proj.server"]
                                                                                                                         :artifacts [{:name "Artifact 1"
                                                                                                                                      :description "Artifact 1"
                                                                                                                                      :scope "art1"
                                                                                                                                      :scope-alias "a1"
                                                                                                                                      :types ["feat", "chore", "refactor"]
                                                                                                                                      :depends-on ["top-z" "top-y"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (count (:depends-on v)) 4))
      (is (= (:depends-on v) [["alpha" [:proj :alpha]] ["bravo" [:proj :bravo]] ["top-z" [:config :project :artifacts 0]] ["top-y" [:config :project :artifacts 0]]])))))


(deftest validate-config-project-specific-test
  (testing "valid config with projects and artifacts"
    (let [v (common/validate-config-project-specific [:config :project] {:config {:project {:name "Top Project"
                                                                                            :scope "proj"
                                                                                            :types ["feat", "chore", "refactor"]
                                                                                            :projects [{:name "Subproject A"
                                                                                                        :scope "proja"
                                                                                                        :types ["feat", "chore", "refactor"]}
                                                                                                       {:name "Subproject B"
                                                                                                        :scope "projb"
                                                                                                        :types ["feat", "chore", "refactor"]}]
                                                                                            :artifacts [{:name "Artifact Y"
                                                                                                         :scope "arty"
                                                                                                         :types ["feat", "chore", "refactor"]}
                                                                                                        {:name "Artifact Z"
                                                                                                         :scope "artz"
                                                                                                         :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "valid config without projects and artifacts"
    (let [v (common/validate-config-project-specific [:config :project] {:config {:project {:name "Top Project"
                                                                                            :scope "proj"
                                                                                            :types ["feat", "chore", "refactor"]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "invalid config: projects is not an array of objects"
    (let [v (common/validate-config-project-specific [:config :project] {:config {:project {:name "Top Project"
                                                                                            :scope "proj"
                                                                                            :types ["feat", "chore", "refactor"]
                                                                                            :projects [1 2 3]
                                                                                            :artifacts [{:name "Artifact Y"
                                                                                                         :scope "arty"
                                                                                                         :types ["feat", "chore", "refactor"]}
                                                                                                        {:name "Artifact Z"
                                                                                                         :scope "artz"
                                                                                                         :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project optional property 'projects' at property 'name' of 'Top Project' and path '[:config :project]' must be an array of objects."))))
  (testing "invalid config: projects is not an array of objects"
    (let [v (common/validate-config-project-specific [:config :project] {:config {:project {:name "Top Project"
                                                                                            :scope "proj"
                                                                                            :types ["feat", "chore", "refactor"]
                                                                                            :projects [{:name "Subproject A"
                                                                                                        :scope "proja"
                                                                                                        :types ["feat", "chore", "refactor"]}
                                                                                                       {:name "Subproject B"
                                                                                                        :scope "projb"
                                                                                                        :types ["feat", "chore", "refactor"]}]
                                                                                            :artifacts [1 2 3]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Project optional property 'artifacts' at property 'name' of 'Top Project' and path '[:config :project]' must be an array of objects.")))))


(deftest validate-config-artifact-specific-test
  (testing "valid config with all optional properties"
    (let [v (common/validate-config-artifact-specific [:config :project :artifacts 0] {:config {:project {:name "Top Project"
                                                                                                            :description "The top project"
                                                                                                            :scope "proj"
                                                                                                            :scope-alias "p"
                                                                                                            :types ["feat", "chore", "refactor"]
                                                                                                            :projects [{:name "Subproject A"
                                                                                                                        :description "The subproject A"
                                                                                                                        :scope "proja"
                                                                                                                        :scope-alias "a"
                                                                                                                        :types ["feat", "chore", "refactor"]}
                                                                                                                       {:name "Subproject B"
                                                                                                                        :description "The subproject B"
                                                                                                                        :scope "projb"
                                                                                                                        :scope-alias "b"
                                                                                                                        :types ["feat", "chore", "refactor"]}]
                                                                                                            :artifacts [{:name "Artifact Y"
                                                                                                                         :description "The artifact Y"
                                                                                                                         :scope "arty"
                                                                                                                         :scope-alias "y"
                                                                                                                         :types ["feat", "chore", "refactor"]}
                                                                                                                        {:name "Artifact Z"
                                                                                                                         :description "The artifact Z"
                                                                                                                         :scope "artz"
                                                                                                                         :scope-alias "z"
                                                                                                                         :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "valid config without optional properties but with 'projects' and 'artifacts"
    (let [v (common/validate-config-artifact-specific [:config :project :artifacts 0] {:config {:project {:name "Top Project"
                                                                                                          :scope "proj"
                                                                                                          :types ["feat", "chore", "refactor"]
                                                                                                          :projects [{:name "Subproject A"
                                                                                                                      :scope "proja"
                                                                                                                      :types ["feat", "chore", "refactor"]}
                                                                                                                     {:name "Subproject B"
                                                                                                                      :scope "projb"
                                                                                                                      :types ["feat", "chore", "refactor"]}]
                                                                                                          :artifacts [{:name "Artifact Y"
                                                                                                                       :scope "arty"
                                                                                                                       :types ["feat", "chore", "refactor"]}
                                                                                                                      {:name "Artifact Z"
                                                                                                                       :scope "artz"
                                                                                                                       :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "invalid config: artifact can't define 'projects'"
    (let [v (common/validate-config-artifact-specific [:config :project :artifacts 0] {:config {:project {:name "Top Project"
                                                                                                          :scope "proj"
                                                                                                          :types ["feat", "chore", "refactor"]
                                                                                                          :projects [{:name "Subproject A"
                                                                                                                      :scope "proja"
                                                                                                                      :types ["feat", "chore", "refactor"]}
                                                                                                                     {:name "Subproject B"
                                                                                                                      :scope "projb"
                                                                                                                      :types ["feat", "chore", "refactor"]}]
                                                                                                          :artifacts [{:name "Artifact Y"
                                                                                                                       :scope "arty"
                                                                                                                       :types ["feat", "chore", "refactor"]
                                                                                                                       :projects [{:name "a"}]}
                                                                                                                      {:name "Artifact Z"
                                                                                                                       :scope "artz"
                                                                                                                       :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact cannot have property 'projects' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]'."))))
  (testing "invalid config: artifact can't define 'artifacts'"
    (let [v (common/validate-config-artifact-specific [:config :project :artifacts 0] {:config {:project {:name "Top Project"
                                                                                                          :scope "proj"
                                                                                                          :types ["feat", "chore", "refactor"]
                                                                                                          :projects [{:name "Subproject A"
                                                                                                                      :scope "proja"
                                                                                                                      :types ["feat", "chore", "refactor"]}
                                                                                                                     {:name "Subproject B"
                                                                                                                      :scope "projb"
                                                                                                                      :types ["feat", "chore", "refactor"]}]
                                                                                                          :artifacts [{:name "Artifact Y"
                                                                                                                       :scope "arty"
                                                                                                                       :types ["feat", "chore", "refactor"]
                                                                                                                       :artifacts [{:name "a"}]}
                                                                                                                      {:name "Artifact Z"
                                                                                                                       :scope "artz"
                                                                                                                       :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact cannot have property 'artifacts' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]'.")))))


(deftest validate-config-artifacts-test
  (testing "valid config: has artifacts"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :scope-alias "ay"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :scope-alias "az"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "valid config: no artifacts"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "invalid config: no name"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:scope "arty"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'name' at path '[:config :project :artifacts 0]' must be a string."))))
  (testing "invalid config: name not a string"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name 5
                                                                                                  :scope "arty"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'name' at path '[:config :project :artifacts 0]' must be a string."))))
  (testing "invalid config: no scope"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'scope' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]' must be a string."))))
  (testing "invalid config: scope not a string"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope 5
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'scope' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]' must be a string."))))
  (testing "invalid config: scope-alias not a string"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :scope-alias 5
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact optional property 'scope-alias' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]' must be a string."))))
  (testing "invalid config: no types"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'types' at property 'name' of 'Artifact Z' and path '[:config :project :artifacts 1]' must be an array of strings."))))
  (testing "invalid config: types not array of strings"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types [{:name "invalid"}]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact required property 'types' at property 'name' of 'Artifact Z' and path '[:config :project :artifacts 1]' must be an array of strings."))))
  (testing "invalid config: defined 'projects'"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :types ["feat", "chore", "refactor"]
                                                                                                  :projects [:name "Invalid Project"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact cannot have property 'projects' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]'."))))
  (testing "invalid config: defined 'artifacts'"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :types ["feat", "chore", "refactor"]
                                                                                                  :artifacts [:name "Invalid Artifact"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= (:reason v) "Artifact cannot have property 'artifacts' at property 'name' of 'Artifact Y' and path '[:config :project :artifacts 0]'."))))
  (testing "depends-on not defined"
    (let [v (common/validate-config-artifacts [:config :project] {:config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :scope-alias "ay"
                                                                                                  :types ["feat", "chore", "refactor"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :scope-alias "az"
                                                                                                  :types ["feat", "chore", "refactor"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (:depends-on v) []))))
  (testing "depends-on defined"
    (let [v (common/validate-config-artifacts [:config :project] {:depends-on [["zzz" [:project :test]]]
                                                                  :config {:project {:name "Top Project"
                                                                                     :scope "proj"
                                                                                     :types ["feat", "chore", "refactor"]
                                                                                     :projects [{:name "Subproject A"
                                                                                                 :scope "proja"
                                                                                                 :types ["feat", "chore", "refactor"]}
                                                                                                {:name "Subproject B"
                                                                                                 :scope "projb"
                                                                                                 :types ["feat", "chore", "refactor"]}]
                                                                                     :artifacts [{:name "Artifact Y"
                                                                                                  :scope "arty"
                                                                                                  :scope-alias "ay"
                                                                                                  :types ["feat", "chore", "refactor"]
                                                                                                  :depends-on ["alpha" "bravo"]}
                                                                                                 {:name "Artifact Z"
                                                                                                  :scope "artz"
                                                                                                  :scope-alias "az"
                                                                                                  :types ["feat", "chore", "refactor"]
                                                                                                  :depends-on ["charlie" "delta"]}]}}})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= (:depends-on v) [["zzz" [:project :test]] ["alpha" [:config :project :artifacts 0]] ["bravo" [:config :project :artifacts 0]] ["charlie" [:config :project :artifacts 1]] ["delta" [:config :project :artifacts 1]]])))))


(deftest get-frequency-on-properties-on-array-of-objects-test
  ;; single property
  (testing "empty target, no duplicates"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [] [:name])]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "single property, no duplicates"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a"} {:name "b"} {:name "c"}][:name])]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "single property, 1 duplicate"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a"} {:name "b"} {:name "c"} {:name "c"}] [:name])]
      (is (seq? v))
      (is (= 1 (count v)))
      (is (= "c" (first v)))))
  (testing "single property, 2 duplicates"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a"} {:name "b"} {:name "c"} {:name "c"} {:name "b"}  {:name "b"}] [:name])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (some #{"b"} v))
      (is (some #{"c"} v))))
  ;; multiple properties
  (testing "multiple properties, no duplicates"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"}] [:name :other])]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "multiple properties, 1 duplicate on same property"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "c" :other "9"}] [:name :other])]
      (is (seq? v))
      (is (= 1 (count v)))
      (is (= "c" (first v)))))
  (testing "multiple properties, 2 duplicates on same properties"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "c" :other "9"} {:name "b" :other "8"}] [:name :other])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (some #{"b"} v))
      (is (some #{"c"} v))))
  (testing "multiple properties, 1 duplicate on different property"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "d" :other "3"}] [:name :other])]
      (is (seq? v))
      (is (= 1 (count v)))
      (is (= "3" (first v)))))
  (testing "multiple properties, 2 duplicates on different properties"
    (let [v (common/get-frequency-on-properties-on-array-of-objects [{:name "a" :other "1"} {:name "b" :other "2"} {:name "c" :other "3"} {:name "c" :other "9"} {:name "z" :other "2"}] [:name :other])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (some #{"2"} v))
      (is (some #{"c"} v)))))


(deftest validate-config-project-artifact-lookahead-test
  (testing "project valid"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "project valid because no nodes"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
  (testing "project invalid: duplicate name"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
                                                                                                                         :projects [{:name "a"
                                                                                                                                     :description "Project A"
                                                                                                                                     :scope "alpha"
                                                                                                                                     :scope-alias "a"}
                                                                                                                                    {:name "b"
                                                                                                                                     :description "Project B"
                                                                                                                                     :scope "bravo"
                                                                                                                                     :scope-alias "b"}
                                                                                                                                    {:name "a"
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= "Project has duplicate value 'a' for required property 'name' at path '[:config :project :projects]'." (:reason v)))))
  (testing "project invalid: duplicate description"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
                                                                                                                         :projects [{:name "a"
                                                                                                                                     :description "Project A"
                                                                                                                                     :scope "alpha"
                                                                                                                                     :scope-alias "a"}
                                                                                                                                    {:name "b"
                                                                                                                                     :description "Project B"
                                                                                                                                     :scope "bravo"
                                                                                                                                     :scope-alias "b"}
                                                                                                                                    {:name "c"
                                                                                                                                     :description "Project A"
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= "Project has duplicate value 'Project A' for optional property 'description' at path '[:config :project :projects]'." (:reason v)))))
  (testing "project invalid: duplicate scope"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
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
                                                                                                                                     :scope "alpha"
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= "Project has duplicate value 'alpha' for required property 'scope' / optional property 'scope-alias' at path '[:config :project :projects]'." (:reason v)))))
  (testing "project invalid: duplicate scope-alias"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
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
                                                                                                                                     :scope-alias "a"}]
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= "Project has duplicate value 'a' for required property 'scope' / optional property 'scope-alias' at path '[:config :project :projects]'." (:reason v)))))
  (testing "project invalid: duplicate scope and scope-alias"
    (let [v (common/validate-config-project-artifact-lookahead :project [:config :project :projects] {:config {:project {:name "top"
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
                                                                                                                                     :scope-alias "alpha"}]
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
                                                                                                                                      :scope-alias "z"}]}}})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= "Project has duplicate value 'alpha' for required property 'scope' / optional property 'scope-alias' at path '[:config :project :projects]'." (:reason v)))))
  (testing "artifact valid"
    (let [v (common/validate-config-project-artifact-lookahead :artifact [:config :project :artifacts] {:config {:project {:name "top"
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
                                                                                                                                        :scope-alias "z"}]}}})]
      (is (map? v))
      (is (true? (:success v)))))
      (testing "both project/artifact valid"
        (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
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
                                                                                                                                                                       :scope-alias "z"}]}}})]
          (is (map? v))
          (is (true? (:success v)))))
          (testing "both project/artifact invalid: same name"
            (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
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
                                                                                                                                                                          {:name "a"
                                                                                                                                                                           :description "Artifact Y"
                                                                                                                                                                           :scope "arty"
                                                                                                                                                                           :scope-alias "y"}
                                                                                                                                                                          {:name "Artifact Z"
                                                                                                                                                                           :description "Artifact Z"
                                                                                                                                                                           :scope "artz"
                                                                                                                                                                           :scope-alias "z"}]}}})]
              (is (map? v))
              (is (false? (:success v)))
              (is (= "Project/Artifact has duplicate value 'a' for required property 'name' at path '[[:config :project :artifacts] [:config :project :projects]]'." (:reason v)))))
              (testing "both project/artifact invalid: same description"
                (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
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
                                                                                                                                                                               :description "Project B"
                                                                                                                                                                               :scope "arty"
                                                                                                                                                                               :scope-alias "y"}
                                                                                                                                                                              {:name "Artifact Z"
                                                                                                                                                                               :description "Artifact Z"
                                                                                                                                                                               :scope "artz"
                                                                                                                                                                               :scope-alias "z"}]}}})]
                  (is (map? v))
                  (is (false? (:success v)))
                  (is (= "Project/Artifact has duplicate value 'Project B' for optional property 'description' at path '[[:config :project :artifacts] [:config :project :projects]]'." (:reason v)))))
                  (testing "both project/artifact invalid: same scope"
                    (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
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
                                                                                                                                                                                   :scope "alpha"
                                                                                                                                                                                   :scope-alias "y"}
                                                                                                                                                                                  {:name "Artifact Z"
                                                                                                                                                                                   :description "Artifact Z"
                                                                                                                                                                                   :scope "artz"
                                                                                                                                                                                   :scope-alias "z"}]}}})]
                      (is (map? v))
                      (is (false? (:success v)))
                      (is (= "Project/Artifact has duplicate value 'alpha' for required property 'scope' / optional property 'scope-alias' at path '[[:config :project :artifacts] [:config :project :projects]]'." (:reason v)))))
                      (testing "both project/artifact invalid: same scope-alias"
                        (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
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
                                                                                                                                                                                       :scope-alias "a"}
                                                                                                                                                                                      {:name "Artifact Z"
                                                                                                                                                                                       :description "Artifact Z"
                                                                                                                                                                                       :scope "artz"
                                                                                                                                                                                       :scope-alias "z"}]}}})]
                          (is (map? v))
                          (is (false? (:success v)))
                          (is (= "Project/Artifact has duplicate value 'a' for required property 'scope' / optional property 'scope-alias' at path '[[:config :project :artifacts] [:config :project :projects]]'." (:reason v)))))
                          (testing "both project/artifact invalid: same project scope and artifact scope-alias"
                            (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
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
                                                                                                                                                                                           :scope-alias "alpha"}
                                                                                                                                                                                          {:name "Artifact Z"
                                                                                                                                                                                           :description "Artifact Z"
                                                                                                                                                                                           :scope "artz"
                                                                                                                                                                                           :scope-alias "z"}]}}})]
                              (is (map? v))
                              (is (false? (:success v)))
                              (is (= "Project/Artifact has duplicate value 'alpha' for required property 'scope' / optional property 'scope-alias' at path '[[:config :project :artifacts] [:config :project :projects]]'." (:reason v)))))
                              (testing "both project/artifact invalid: same project scope-alias and artifact scope"
                                (let [v (common/validate-config-project-artifact-lookahead :both [[:config :project :artifacts] [:config :project :projects]] {:config {:project {:name "top"
                                                                                                                                                                                  :projects [{:name "a"
                                                                                                                                                                                              :description "Project A"
                                                                                                                                                                                              :scope "arty"
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
                                                                                                                                                                                               :scope-alias "z"}]}}})]
                                  (is (map? v))
                                  (is (false? (:success v)))
                                  (is (= "Project/Artifact has duplicate value 'arty' for required property 'scope' / optional property 'scope-alias' at path '[[:config :project :artifacts] [:config :project :projects]]'." (:reason v))))))


;; a full config to use for more comprehensive tests
(def config
  {:commit-msg-enforcement {:enabled true}
   :commit-msg {:length {:title-line {:min 3
                                      :max 20}
                         :body-line {:min 2
                                     :max 10}}}
   :project {:name "Root Project"
             :description "The Root Project"
             :scope "proj"
             :scope-alias "p"
             :types ["feat", "chore", "refactor"]
             :artifacts [{:name "Root Project Artifact 1"
                          :description "The Root Project Artifact 1"
                          :scope "root-a1"
                          :scope-alias "ra1"
                          :types ["feat", "chore", "refactor"]}
                         {:name "Root Project Artifact 2"
                          :description "The Root Project Artifact 2"
                          :scope "root-a2"
                          :scope-alias "ra2"
                          :types ["feat", "chore", "refactor"]}
                         {:name "Root Project Artifact 3"
                          :description "The Root Project Artifact 3"
                          :scope "root-a3"
                          :scope-alias "ra3"
                          :types ["feat", "chore", "refactor"]}]
             :projects [{:name "Alpha Project"
                         :description "The Alpha Project"
                         :scope "alpha-p"
                         :scope-alias "a"
                         :types ["feat", "chore", "refactor"]
                         :artifacts [{:name "Alpha Artifact1"
                                      :description "The Alpha Artifact1"
                                      :scope "alpha-art1"
                                      :scope-alias "a-a1"
                                      :types ["feat", "chore", "refactor"]}
                                     {:name "Alpha Artifact2"
                                      :description "The Alpha Artifact2"
                                      :scope "alpha-art2"
                                      :scope-alias "a-a2"
                                      :types ["feat", "chore", "refactor"]}
                                     {:name "Alpha Artifact3"
                                      :description "The Alpha Artifact3"
                                      :scope "alpha-art3"
                                      :scope-alias "a-a3"
                                      :types ["feat", "chore", "refactor"]}]
                         :projects [{:name "Alpha Subproject1"
                                     :description "The Alpha Subproject1"
                                     :scope "alpha-subp1"
                                     :scope-alias "as1"
                                     :types ["feat", "chore", "refactor"]
                                     :artifacts [{:name "Alpha Sub Artifact1-1"
                                                  :description "The Alpha Sub Artifact1-1"
                                                  :scope "alpha-sart1-1"
                                                  :scope-alias "a-sa1-1"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Alpha Sub Artifact1-2"
                                                  :description "The Alpha Sub Artifact1-2"
                                                  :scope "alpha-sart1-2"
                                                  :scope-alias "a-sa1-2"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Alpha Sub Artifact1-3"
                                                  :description "The Alpha Sub Artifact1-3"
                                                  :scope "alpha-sart1-3"
                                                  :scope-alias "a-sa1-3"
                                                  :types ["feat", "chore", "refactor"]}]}
                                    {:name "Alpha Subproject2"
                                     :description "The Alpha Subproject2"
                                     :scope "alpha-subp2"
                                     :scope-alias "as2"
                                     :types ["feat", "chore", "refactor"]
                                     :artifacts [{:name "Alpha Sub Artifact2-1"
                                                  :description "The Alpha Sub Artifact2-1"
                                                  :scope "alpha-sart2-1"
                                                  :scope-alias "a-sa2-1"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Alpha Sub Artifact2-2"
                                                  :description "The Alpha Sub Artifact2-2"
                                                  :scope "alpha-sart2-2"
                                                  :scope-alias "a-sa2-2"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Alpha Sub Artifact2-3"
                                                  :description "The Alpha Sub Artifact2-3"
                                                  :scope "alpha-sart2-3"
                                                  :scope-alias "a-sa1-3"
                                                  :types ["feat", "chore", "refactor"]}]}
                                    {:name "Alpha Subproject3"
                                     :description "The Alpha Subproject3"
                                     :scope "alpha-subp3"
                                     :scope-alias "as3"
                                     :types ["feat", "chore", "refactor"]
                                     :artifacts [{:name "Alpha Sub Artifact3-1"
                                                  :description "The Alpha Sub Artifact3-1"
                                                  :scope "alpha-sart3-1"
                                                  :scope-alias "a-sa3-1"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Alpha Sub Artifact3-2"
                                                  :description "The Alpha Sub Artifact3-2"
                                                  :scope "alpha-sart3-2"
                                                  :scope-alias "a-sa3-2"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Alpha Sub Artifact3-3"
                                                  :description "The Alpha Sub Artifact3-3"
                                                  :scope "alpha-sart3-3"
                                                  :scope-alias "a-sa3-3"
                                                  :types ["feat", "chore", "refactor"]}]}]}
                        {:name "Bravo Project"
                         :description "The Bravo Project"
                         :scope "bravo-p"
                         :scope-alias "b"
                         :types ["feat", "chore", "refactor"]
                         :artifacts [{:name "Bravo Artifact1"
                                      :description "The Bravo Artifact1"
                                      :scope "bravo-art1"
                                      :scope-alias "b-a1"
                                      :types ["feat", "chore", "refactor"]}
                                     {:name "Bravo Artifact2"
                                      :description "The Bravo Artifact2"
                                      :scope "bravo-art2"
                                      :scope-alias "b-a2"
                                      :types ["feat", "chore", "refactor"]}
                                     {:name "Bravo Artifact3"
                                      :description "The Bravo Artifact3"
                                      :scope "bravo-art3"
                                      :scope-alias "b-a3"
                                      :types ["feat", "chore", "refactor"]}]
                         :projects [{:name "Bravo Subproject1"
                                     :description "The Bravo Subproject1"
                                     :scope "bravo-subp1"
                                     :scope-alias "bs1"
                                     :types ["feat", "chore", "refactor"]
                                     :artifacts [{:name "Bravo Sub Artifact1-1"
                                                  :description "The Bravo Sub Artifact1-1"
                                                  :scope "bravo-bart1-1"
                                                  :scope-alias "b-sa1-1"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Bravo Sub Artifact1-2"
                                                  :description "The Bravo Sub Artifact1-2"
                                                  :scope "bravo-bart1-2"
                                                  :scope-alias "b-sa1-2"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Bravo Sub Artifact1-3"
                                                  :description "The Bravo Sub Artifact1-3"
                                                  :scope "bravo-bart1-3"
                                                  :scope-alias "b-sa1-3"
                                                  :types ["feat", "chore", "refactor"]}]}
                                    {:name "Bravo Subproject2"
                                     :description "The Bravo Subproject2"
                                     :scope "bravo-subp2"
                                     :scope-alias "bs2"
                                     :types ["feat", "chore", "refactor"]
                                     :artifacts [{:name "Bravo Sub Artifact2-1"
                                                  :description "The Bravo Sub Artifact2-1"
                                                  :scope "bravo-bart2-1"
                                                  :scope-alias "b-sa2-1"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Bravo Sub Artifact2-2"
                                                  :description "The Bravo Sub Artifact2-2"
                                                  :scope "bravo-bart2-2"
                                                  :scope-alias "b-sa2-2"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Bravo Sub Artifact2-3"
                                                  :description "The Bravo Sub Artifact2-3"
                                                  :scope "bravo-bart2-3"
                                                  :scope-alias "b-sa1-3"
                                                  :types ["feat", "chore", "refactor"]}]}
                                    {:name "Bravo Subproject3"
                                     :description "The Bravo Subproject3"
                                     :scope "bravo-subp3"
                                     :scope-alias "bs3"
                                     :types ["feat", "chore", "refactor"]
                                     :artifacts [{:name "Bravo Sub Artifact3-1"
                                                  :description "The Bravo Sub Artifact3-1"
                                                  :scope "bravo-bart3-1"
                                                  :scope-alias "b-sa3-1"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Bravo Sub Artifact3-2"
                                                  :description "The Bravo Sub Artifact3-2"
                                                  :scope "bravo-bart3-2"
                                                  :scope-alias "b-sa3-2"
                                                  :types ["feat", "chore", "refactor"]}
                                                 {:name "Bravo Sub Artifact3-3"
                                                  :description "The Bravo Sub Artifact3-3"
                                                  :scope "bravo-bart3-3"
                                                  :scope-alias "b-sa3-3"
                                                  :types ["feat", "chore", "refactor"]}]}]}]}})



;; the testing for this function focuses on complete traversal of the graph with comprehensive error cases deferred to the constituent functions
(deftest validate-config-projects-test
  (testing "valid config: full config that includes multiple layers of sub-projects with artifacts"
    (let [v (common/validate-config-projects {:config config})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "valid config: root project without artifacts or sub-projects"
    (let [v (common/validate-config-projects {:config (dissoc (dissoc config [:project :projects]) [:project :artifacts])})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "valid config: root project with artifacts but without sub-projects"
    (let [v (common/validate-config-projects {:config (dissoc config [:project :projects])})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "valid config: root project with sub-projects but without artifacts"
    (let [v (common/validate-config-projects {:config (dissoc config [:project :artifacts])})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "invalid config: traversed to first node of max depth (project Alpha Subproject 1) in BFS traversal of graph and first artifact (Alpha Sub Artifact1-1) which has invalid scope"
    (let [v (common/validate-config-projects {:config (assoc-in config [:project :projects 0 :projects 0 :artifacts 0 :scope] 5)})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Alpha Sub Artifact1-1' and path '[:config :project :projects 0 :projects 0 :artifacts 0]' must be a string." (:reason v)))))
  (testing "invalid config: traversed to first node of max depth (project Alpha Subproject 1) in BFS traversal of graph and last artifact (Alpha Sub Artifact1-3) which has invalid scope"
    (let [v (common/validate-config-projects {:config (assoc-in config [:project :projects 0 :projects 0 :artifacts 2 :scope] 5)})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Alpha Sub Artifact1-3' and path '[:config :project :projects 0 :projects 0 :artifacts 2]' must be a string." (:reason v)))))
  (testing "invalid config: traversed to last BFS leaf node (project Bravo Subproject 3) and last artifact (Bravo Sub Artifact3-1) which has invalid scope"
    (let [v (common/validate-config-projects {:config (assoc-in config [:project :projects 1 :projects 2 :artifacts 0 :scope] 5)})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Bravo Sub Artifact3-1' and path '[:config :project :projects 1 :projects 2 :artifacts 0]' must be a string." (:reason v)))))
  (testing "invalid config: traversed to last BFS leaf node (project Bravo Subproject 3) and last artifact (Bravo Sub Artifact3-3) which has invalid scope"
    (let [v (common/validate-config-projects {:config (assoc-in config [:project :projects 1 :projects 2 :artifacts 2 :scope] 5)})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Bravo Sub Artifact3-3' and path '[:config :project :projects 1 :projects 2 :artifacts 2]' must be a string." (:reason v)))))
  (testing "depends-on"
    (let [v (common/validate-config-projects {:config (-> config
                                                          (assoc-in [:project :projects 1 :artifacts 0 :depends-on] ["alpha"])
                                                          (assoc-in [:project :projects 0 :depends-on] ["bravo"]))})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:depends-on v) [["bravo" [:config :project :projects 0]] ["alpha" [:config :project :projects 1 :artifacts 0]]])))))


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
                         :projects[{:name "bb"
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


(deftest update-children-get-next-child-scope-path-test
  (let [config get-child-nodes-including-depends-on-test-config]
    ;;
    ;; no children
    (testing "project: no children"
      (let [cur-node-json-path [:project :projects 0]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (is (nil? (:scope-path v)))))
    (testing "artifact: no children (e.g., no depends-on)"
      (let [cur-node-json-path [:project :arifacts 0]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (is (nil? (:scope-path v)))))
    (testing "no children: two calls"
      (let [cur-node-json-path [:project :projects 0]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (nil? (:scope-path v)))
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (let [v2 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v))]
          (is (true? (get-in v2 (concat [:config] (conj cur-node-json-path :visited)))))
          (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
          (is (nil? (:scope-path v2))))))
    ;;
    ;; 1 child
    (testing "project: 1 child project"
      (let [cur-node-json-path [:project :projects 1]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (is (= "top.bravo.bravo2" (:scope-path v)))))
    (testing "project: 1 child artifact"
      (let [cur-node-json-path [:project :projects 2]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (is (= "top.charlie.artcfrx" (:scope-path v)))))
    (testing "project: 1 child depends-on"
      (let [cur-node-json-path [:project :projects 0]
            config (assoc-in config (conj cur-node-json-path :depends-on) ["top.alpha"])
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (is (= "top.alpha" (:scope-path v)))))
    (testing "1 child: 2 calls"
      (let [cur-node-json-path [:project :projects 1]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
        (is (= "top.bravo.bravo2" (:scope-path v)))
        (let [v2 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v))]
         (is (true? (get-in v2 (concat [:config] (conj cur-node-json-path :visited)))))
         (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
         (is (nil? (:scope-path v2))))))
    ;;
    ;; multiple children
    (testing "multiple children (total of 6): 2 each of: projects, artifacts, depends-on"
      ;;
      ;; 1st call
      (let [cur-node-json-path [:project :projects 3]
            v (common/update-children-get-next-child-scope-path cur-node-json-path config)]
        (is (true? (get-in v (concat [:config] (conj cur-node-json-path :visited)))))
        (is (= (count (get-in v (concat [:config] (conj cur-node-json-path :unvisited-children)))) 5))
        (is (= "top.delta.ad1" (:scope-path v)))
        ;;
        ;; 2nd call
        (let [v2 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v))]
         (is (true? (get-in v2 (concat [:config] (conj cur-node-json-path :visited)))))
         (is (= (count (get-in v2 (concat [:config] (conj cur-node-json-path :unvisited-children)))) 4))
         (is (= "top.delta.ad2" (:scope-path v2)))
          ;;
          ;; 3rd call
          (let [v3 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v2))]
           (is (true? (get-in v3 (concat [:config] (conj cur-node-json-path :visited)))))
           (is (= (count (get-in v3 (concat [:config] (conj cur-node-json-path :unvisited-children)))) 3))
           (is (= "top.delta.d1" (:scope-path v3)))
            ;;
            ;; 4th call
            (let [v4 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v3))]
             (is (true? (get-in v4 (concat [:config] (conj cur-node-json-path :visited)))))
             (is (= (count (get-in v4 (concat [:config] (conj cur-node-json-path :unvisited-children)))) 2))
             (is (= "top.delta.d2" (:scope-path v4)))
              ;;
              ;; 5th call
              (let [v5 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v4))]
               (is (true? (get-in v5 (concat [:config] (conj cur-node-json-path :visited)))))
               (is (= (count (get-in v5 (concat [:config] (conj cur-node-json-path :unvisited-children)))) 1))
               (is (= "top.alpha" (:scope-path v5)))
                ;;
                ;; 6th call
                (let [v6 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v5))]
                 (is (true? (get-in v6 (concat [:config] (conj cur-node-json-path :visited)))))
                 (is (= (count (get-in v6 (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
                 (is (= "top.bravo" (:scope-path v6)))
                  ;;
                  ;; 7th call
                  (let [v7 (common/update-children-get-next-child-scope-path cur-node-json-path (:config v6))]
                   (is (true? (get-in v7 (concat [:config] (conj cur-node-json-path :visited)))))
                   (is (= (count (get-in v7 (concat [:config] (conj cur-node-json-path :unvisited-children)))) 0))
                   (is (nil? (:scope-path v7)))))))))))))


(deftest add-full-paths-to-config-test
  (testing "full config"
    (let [v (common/add-full-paths-to-config config)]
      
      ;; root level - project
      (is (= (get-in v [:project :full-json-path]) [:project]))
      (is (= (get-in v [:project :full-scope-path]) ["proj"]))
      (is (= (get-in v [:project :full-scope-path-formatted]) "proj"))

      ;; root level - artifact
      (is (= (get-in v [:project :artifacts 0 :full-json-path]) [:project :artifacts 0]))
      (is (= (get-in v [:project :artifacts 0 :full-scope-path]) ["proj" "root-a1"]))
      (is (= (get-in v [:project :artifacts 0 :full-scope-path-formatted]) "proj.root-a1"))

      ;; level 1 - project
      (is (= (get-in v [:project :projects 0 :full-json-path]) [:project :projects 0]))
      (is (= (get-in v [:project :projects 0 :full-scope-path]) ["proj" "alpha-p"]))
      (is (= (get-in v [:project :projects 0 :full-scope-path-formatted]) "proj.alpha-p"))

      ;; level 1 - artifact
      (is (= (get-in v [:project :projects 0 :artifacts 0 :full-json-path]) [:project :projects 0 :artifacts 0]))
      (is (= (get-in v [:project :projects 0 :artifacts 0 :full-scope-path]) ["proj" "alpha-p" "alpha-art1"]))
      (is (= (get-in v [:project :projects 0 :artifacts 0 :full-scope-path-formatted]) "proj.alpha-p.alpha-art1")))))


(def validate-config-depends-on-test-config
  {:project {:name "top"
             :scope "top"
             :projects [{:name "a"
                         :description "Project A"
                         :scope "alpha"
                         :scope-alias "a"}
                        {:name "b"
                         :description "Project B"
                         :scope "bravo"
                         :scope-alias "b",
                         :projects [{:name "bb"
                                     :description "Project BB"
                                     :scope "bravo2"
                                     :scope-alias "b2"}]}
                        {:name "c"
                         :description "Project C"
                         :scope "charlie"
                         :scope-alias "c"
                         :artifacts [{:name "Artifact C from X"
                                      :description "Artifact C from X"
                                      :scope "artcfrx"
                                      :scope-alias "cfrx"}]}
                        {:name "d"
                         :description "Project D"
                         :scope "delta"
                         :scope-alias "d"
                         :projects [{:name "Project D1"
                                     :description "Project D1"
                                     :scope "d1"
                                     :scope-alias "d1"}
                                    {:name "Project D2"
                                     :description "Project D2"
                                     :scope "d2"
                                     :scope-alias "d2"}]
                         :artifacts [{:name "Artifact AD1"
                                      :description "Artifact AD1"
                                      :scope "ad1"
                                      :scope-alias "ad1"}
                                     {:name "Artifact AD2"
                                      :description "Artifact AD2"
                                      :scope "ad2"
                                      :scope-alias "ad2"}]}]
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


;; for cycle detection tests:  only 'depends-on' *could* create a cycle
(deftest validate-config-depends-on-test
  (testing "no cycle, no depends-on"
    (let [data {:success true :config validate-config-depends-on-test-config}
          v (common/validate-config-depends-on data)]
      (is (true? (:success v)))
      (is (= (get-in v [:config :project :name]) "top"))))
  (testing "no cycle, w/ depends-on"
    (let [config (assoc-in validate-config-depends-on-test-config [:project :projects 3 :projects 0 :depends-on] ["top.delta.d2"])
          data {:success true :config config}
          v (common/validate-config-depends-on data)]
      (is (true? (:success v)))
      (is (= (get-in v [:config :project :name]) "top"))))
  (testing "cycle: depends-on causes cycle between siblings"
    (let [config (assoc-in (assoc-in validate-config-depends-on-test-config [:project :projects 3 :projects 0 :depends-on] ["top.delta.d2"]) [:project :projects 3 :projects 1 :depends-on] ["top.delta.d1"])
          data {:success true :config config}
          v (common/validate-config-depends-on data)]
      (is (false? (:success v)))
      (is (= (:reason v) "Cycle detected at traversal path '[\"top\" \"top.delta\" \"top.delta.d1\" \"top.delta.d2\"]' with scope path '[:project :projects 3 :projects 0]' for scope 'top.delta.d1'."))))
  (testing "cycle: depends-on causes cycle between parent/child"
    (let [config (assoc-in validate-config-depends-on-test-config [:project :projects 3 :projects 0 :depends-on] ["top.delta"])
          data {:success true :config config}
          v (common/validate-config-depends-on data)]
      (is (false? (:success v)))
      (is (= (:reason v) "Cycle detected at traversal path '[\"top\" \"top.delta\" \"top.delta.d1\"]' with scope path '[:project :projects 3]' for scope 'top.delta'."))))
  (testing "cycle: depends-on causes cycle between ancestor/descendant"
    (let [config (assoc-in validate-config-depends-on-test-config [:project :projects 3 :projects 0 :depends-on] ["top"])
          data {:success true :config config}
          v (common/validate-config-depends-on data)]
      (is (false? (:success v)))
      (is (= (:reason v) "Cycle detected at traversal path '[\"top\" \"top.delta\" \"top.delta.d1\"]' with scope path '[:project]' for scope 'top'.")))))


;; Comprehensive error cases deferred to the constituent functions.  The testing for this function focuses on:
;; - validation of config header, root project, and sub-projects
;; - complete traversal of the graph
;; - demonsrate cycle detection
(deftest validate-config-test
  ;; commit message enforcement block
  (testing "enforcement block not defined"
    (let [v (common/validate-config {})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= "Commit message enforcement block (commit-msg-enforcement) must be defined." (:reason v)))
      (is (true? (contains? v :config)))))
  (testing "'enabled' not defined"
    (let [v (common/validate-config {:commit-msg-enforcement {}})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= "Commit message enforcement must be set as enabled or disabled (commit-msg-enforcement.enabled) with either 'true' or 'false'." (:reason v)))
      (is (true? (contains? v :config)))))
  (testing "'enabled' set to nil"
    (let [v (common/validate-config {:commit-msg-enforcement {:enabled nil}})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= "Commit message enforcement must be set as enabled or disabled (commit-msg-enforcement.enabled) with either 'true' or 'false'." (:reason v)))
      (is (true? (contains? v :config)))))
  ;; min/max line lengths
  (testing "title-line.min is not defined"
    (let [v (common/validate-config {:commit-msg-enforcement {:enabled true} :commit-msg {:length {:title-line {:max 20}
                                                                                                   :body-line {:min 2
                                                                                                               :max 10}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of title line (length.title-line.min) must be defined.")))))
  (testing "title-line.max is not defined"
    (let [v (common/validate-config {:commit-msg-enforcement {:enabled true} :commit-msg {:length {:title-line {:min 12}
                                                                                                   :body-line {:min 2
                                                                                                               :max 10}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of title line (length.title-line.max) must be defined.")))))
  (testing "body-line.min is not defined"
    (let [v (common/validate-config {:commit-msg-enforcement {:enabled true} :commit-msg {:length {:title-line {:min 12
                                                                                                                :max 20}
                                                                                                   :body-line {:max 10}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Minimum length of body line (length.body-line.min) must be defined.")))))
  (testing "body-line.max is not defined"
    (let [v (common/validate-config {:commit-msg-enforcement {:enabled true} :commit-msg {:length {:title-line {:min 12
                                                                                                                :max 20}
                                                                                                   :body-line {:min 2}}}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (= (:reason v) "Maximum length of body line (length.body-line.max) must be defined.")))))
  ;; root project
  (testing "project invalid: property 'project' not defined"
    (let [v (common/validate-config {:commit-msg-enforcement {:enabled true}
                                     :commit-msg {:length {:title-line {:min 12
                                                                        :max 20}
                                                           :body-line {:min 2
                                                                       :max 10}}}
                                     :not-project {:a 1 :b 2}})]
      (is (map? v))
      (is (true? (contains? v :config)))
      (is (false? (:success v)))
      (is (= (:reason v) "Property 'project' must be defined at the top-level."))))
  ;; sub-projects
  (testing "valid config: full config that includes multiple layers of sub-projects with artifacts"
    (let [v (common/validate-config config)]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "valid config: root project without artifacts or sub-projects"
    (let [v (common/validate-config (dissoc (dissoc config [:config :project :projects]) [:config :project :artifacts]))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "valid config: root project with artifacts but without sub-projects"
    (let [v (common/validate-config (dissoc config [:config :project :projects]))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "invalid config: traversed to first node of max depth (project Alpha Subproject 1) in BFS traversal of graph and first artifact (Alpha Sub Artifact1-1) which has invalid scope"
    (let [v (common/validate-config (assoc-in config [:project :projects 0 :projects 0 :artifacts 0 :scope] 5))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Alpha Sub Artifact1-1' and path '[:config :project :projects 0 :projects 0 :artifacts 0]' must be a string." (:reason v)))))
  (testing "invalid config: traversed to first node of max depth (project Alpha Subproject 1) in BFS traversal of graph and last artifact (Alpha Sub Artifact1-3) which has invalid scope"
    (let [v (common/validate-config (assoc-in config [:project :projects 0 :projects 0 :artifacts 2 :scope] 5))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Alpha Sub Artifact1-3' and path '[:config :project :projects 0 :projects 0 :artifacts 2]' must be a string." (:reason v)))))
  (testing "invalid config: traversed to last BFS leaf node (project Bravo Subproject 3) and last artifact (Bravo Sub Artifact3-1) which has invalid scope"
    (let [v (common/validate-config (assoc-in config [:project :projects 1 :projects 2 :artifacts 0 :scope] 5))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Bravo Sub Artifact3-1' and path '[:config :project :projects 1 :projects 2 :artifacts 0]' must be a string." (:reason v)))))
  (testing "invalid config: traversed to last BFS leaf node (project Bravo Subproject 3) and last artifact (Bravo Sub Artifact3-3) which has invalid scope"
    (let [v (common/validate-config (assoc-in config [:project :projects 1 :projects 2 :artifacts 2 :scope] 5))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Artifact required property 'scope' at property 'name' of 'Bravo Sub Artifact3-3' and path '[:config :project :projects 1 :projects 2 :artifacts 2]' must be a string." (:reason v)))))
  (testing "invalid config: cycle detected"
    (let [v (common/validate-config (assoc-in config [:project :projects 0 :depends-on] ["proj"]))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Cycle detected at traversal path '[\"proj\" \"proj.alpha-p\"]' with scope path '[:project]' for scope 'proj'." (:reason v))))))
   

(deftest config-enabled?-test
  (testing "enabled"
    (let [v (common/config-enabled? {:commit-msg-enforcement {:enabled true}})]
      (is (true? v))
      (is (boolean? v))))
  (testing "disabled"
    (let [v (common/config-enabled? {:commit-msg-enforcement {:enabled false}})]
      (is (false? v))
      (is (boolean? v)))))


(deftest read-file-test
  (testing "file not found"
    (let [v (common/read-file (str resources-test-data-dir-string "/" "does-not-exist.txt"))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (true? (str/includes? (:reason v) "File 'test/resources/data/does-not-exist.txt' not found."))))) 
  (testing "file ok"
    (let [v (common/read-file (str resources-test-data-dir-string "/" "file-to-read.txt"))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (string? (:result v)))
      (is (= "This is a\n\nmulti-line file to read\n" (:result v))))))


(deftest write-file-test
  (let [test-dir-string temp-dir-string]
    (testing "file not found"
      (let [v (common/write-file (str test-dir-string "/does-not-exist/file.txt") "Line 1\nLine 2\nLine 3")]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "File '" test-dir-string "/does-not-exist/file.txt' not found."))))))
    (testing "file ok"
      (let [content "Line 1\nLine 2\nLine 3"
            out-file-string (str test-dir-string "/write-file-ok.txt")
            v (common/write-file out-file-string content)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (= content (slurp out-file-string)))))))


(deftest format-commit-msg-all-test
  (testing "empty string"
    (let [v (common/format-commit-msg-all "")]
      (is (= "" v))
      (is (string? v))))
  (testing "replace all lines with comments with empty strings"
    (let [v (common/format-commit-msg-all "#Comment0\nLine1\n#Comment2\nLine3\n #Comment4\nLine5\n#  Comment 6\nLine7")]
      (is (= "Line1\n\nLine3\n\nLine5\n\nLine7" v))
      (is (string? v))))
  (testing "for a line with spaces only, remove all spaces"
    (let [v (common/format-commit-msg-all "Line1\n \nLine3\n   \nLine5")]
      (is (= "Line1\n\nLine3\n\nLine5" v))
      (is (string? v))))
  (testing "replace two or more consecutive newlines with a single newline"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (common/format-commit-msg-all "Line1\nLine2\n\nLine3\n\nLine4\nLine5\nLine6\n\n\nLine7\n\n\n\n\n\nLine8")]
      (is (= "Line1\n\nLine2\n\nLine3\n\nLine4\nLine5\nLine6\n\nLine7\n\nLine8" v))
      (is (string? v))))
  (testing "remove spaces at end of lines (without removing spaces at beginning of lines)"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (common/format-commit-msg-all "Line1\nLine2  \n  Line3  \nLine4\n Line5 ")]
      (is (= "Line1\n\nLine2\n  Line3\nLine4\n Line5" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE: <msg>' formatted correctly"
    (let [v (common/format-commit-msg-all "BREAKING CHANGE: a change")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' with spaces"
    (let [v (common/format-commit-msg-all "  BREAKING CHANGE  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' lowercase"
    (let [v (common/format-commit-msg-all "  breaking change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' mixed case"
    (let [v (common/format-commit-msg-all "  BreaKing chANge  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' separated with underscore"
    (let [v (common/format-commit-msg-all "  breaking_change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' separated with dash"
    (let [v (common/format-commit-msg-all "  breaking-change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' misspeled braking"
    (let [v (common/format-commit-msg-all "  braking change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' misspeled braeking"
    (let [v (common/format-commit-msg-all "  braeking change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "remove leading/trailing spaces"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (common/format-commit-msg-all "  Line1\nTest\nLine2  ")]
      (is (= "Line1\n\nTest\nLine2" v))
      (is (string? v))))
  (testing "remove leading/trailing newlines"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (common/format-commit-msg-all "\nLine1\nTest\nLine2\n")]
      (is (= "Line1\n\nTest\nLine2" v))
      (is (string? v)))))


(deftest format-commit-msg-first-line-test
  (testing "empty string"
    (let [v (common/format-commit-msg-first-line "")]
      (is (= "" v))
      (is (string? v))))
  (testing "without exclamation mark"
    (let [v (common/format-commit-msg-first-line "    feat  (  client  )    :      add super neat feature   ")]
      (is (= "feat(client): add super neat feature" v))
      (is (string? v))))
  (testing "with exclamation mark"
    (let [v (common/format-commit-msg-first-line "    feat  (  client  )  !  :      add super neat feature   ")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v)))))


(def long-commit-msg
 "
   feat  (  client  )  !  :      add super neat feature   
Support new data with addition of super neat feature

Another line
Directly after line

# Comment line

     # Another comment line

Another line

     This line has 5 spaces before, which is ok

This line has 5 spaces after this     

Line with 4 spaces only below
    
Last real line


breaking change: a big change
BREAKING CHANGE: a big change
BreakinG ChangE: a big change

braking change: a big change
braeking change: a big change

breaking   change: a big change
breaking_change: a big change
breaking-change: a big change

breaking change    :    a big change

# Please enter the commit message for your changes. Lines starting
# with '#' will be ignored, and an empty message aborts the commit.
#
# On branch main
# Your branch is up to date with 'origin/main'.
#
# Changes to be committed:
#	modified:   client-side-hooks/src/commit-msg
#


")


(def long-commit-msg-expected
 "feat(client)!: add super neat feature

Support new data with addition of super neat feature

Another line
Directly after line

Another line

     This line has 5 spaces before, which is ok

This line has 5 spaces after this

Line with 4 spaces only below

Last real line

BREAKING CHANGE: a big change
BREAKING CHANGE: a big change
BREAKING CHANGE: a big change

BREAKING CHANGE: a big change
BREAKING CHANGE: a big change

BREAKING CHANGE: a big change
BREAKING CHANGE: a big change
BREAKING CHANGE: a big change

BREAKING CHANGE: a big change")


(deftest format-commit-msg-test
  (testing "nil string"
    (let [v (common/format-commit-msg nil)]
      (is (= "" v))
      (is (string? v))))
  (testing "empty string"
    (let [v (common/format-commit-msg "")]
      (is (= "" v))
      (is (string? v))))
  (testing "one line"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   ")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one line and newline"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one line and multiple newlines"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n\n\n")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one line and comment"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n#Comment here")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one newline then body"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \nBody starts here")]
      (is (= "feat(client)!: add super neat feature\n\nBody starts here" v))
      (is (string? v))))
  (testing "two newlines then body"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n\nBody starts here")]
      (is (= "feat(client)!: add super neat feature\n\nBody starts here" v))
      (is (string? v))))
  (testing "three newlines then body"
    (let [v (common/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n\n\nBody starts here")]
      (is (= "feat(client)!: add super neat feature\n\nBody starts here" v))
      (is (string? v))))
  (testing "long commit message"
    (let [v (common/format-commit-msg long-commit-msg)]
      (is (= long-commit-msg-expected v))
      (is (string? v)))))


(deftest index-matches-test
  (testing "empty collection"
    (let [v (common/index-matches [] #"z")]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "no matches"
    (let [v (common/index-matches ["aqq" "bqq" "cqq" "dqq"] #"z")]
      (is (seq? v))
      (is (= 0 (count v)))))
  (testing "1 match with collection count 1"
    (let [v (common/index-matches ["bqq"] #"b")]
      (is (seq? v))
      (is (= 1 (count v)))))
  (testing "1 match with collection count 5"
    (let [v (common/index-matches ["aqq" "bqq" "cqq" "dqq"] #"a")]
      (is (seq? v))
      (is (= 1 (count v)))))
  (testing "4 matches with collection count 7"
    (let [v (common/index-matches ["aqq" "bqq" "acqq" "dqq" "eqq" "faqq" "gaqq"] #"a")]
      (is (seq? v))
      (is (= 4 (count v))))))


(deftest create-validate-commit-msg-err-test
  (testing "reason without locations"
    (let [v (common/create-validate-commit-msg-err "Reason error occurred")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (string? (:reason v)))
      (is (false? (contains? v :locations)))))
  (testing "reason with locations"
    (let [v (common/create-validate-commit-msg-err "Reason error occurred" '(3 7 9))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (string? (:reason v)))
      (is (= 3 (count (:locations v))))
      (is (= 3 (first (:locations v))))
      (is (= 7 (nth (:locations v) 1)))
      (is (= 9 (nth (:locations v) 2))))))


(deftest validate-commit-msg-title-len-test
  (let [config {:commit-msg {:length {:title-line {:min 12    ;; 'ab(cd): efgh' = 12 chars
                                                   :max 20}
                                      :body-line {:min 2
                                                  :max 10}}}}]
    (testing "commit msg title line has too few characters"
      (let [v (common/validate-commit-msg-title-len "ab(cd): efg" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must be at least " (:min (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg title line meets minimum characters"
      (let [v (common/validate-commit-msg-title-len "ab(cd): efgh" config)]
        (is (nil? v))))
    (testing "commit msg title line has too many characters"
      (let [v (common/validate-commit-msg-title-len "ab(cd): efghijklmnopq" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must not contain more than " (:max (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg title line meets maximum characters"
      (let [v (common/validate-commit-msg-title-len "ab(cd): efghijklmnop" config)]
        (is (nil? v))))))


(deftest validate-commit-msg-body-len-test
  (let [config {:commit-msg {:length {:title-line {:min 3
                                                   :max 8}
                                      :body-line {:min 2
                                                  :max 10}}}}]
    (testing "commit msg body is an empty sequence"
      (let [v (common/validate-commit-msg-body-len [] config)]
        (is (nil? v))))
    (testing "commit msg body line has too few characters, for single element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "L") config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg body line has too few characters, for multi element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "L\nHello\nA\nAnother line\nX") config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 3 (count (:locations v))))
        (is (= 0 (first (:locations v))))
        (is (= 2 (nth (:locations v) 1)))
        (is (= 4 (nth (:locations v) 2)))))
    (testing "commit msg body line has meets minimum characters, for single element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "Li") config)]
        (is (nil? v))))
    (testing "commit msg body line has meets minimum characters, for multi element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "Li\nAb\nAbcdef\nAb\nAb") config)]
        (is (nil? v))))
    (testing "commit msg body line has too many characters, for single element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "Body abcdef") config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg body line has too many characters, for multi element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "Body abcdef\nAbcd\nBody abcdef\nBody abcdef\nAbcd\nBody abcdef") config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 4 (count (:locations v))))
        (is (= 0 (first (:locations v))))
        (is (= 2 (nth (:locations v) 1)))
        (is (= 3 (nth (:locations v) 2)))
        (is (= 5 (nth (:locations v) 3)))))
    (testing "commit msg body line has meets maximum characters, for single element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "Body abcde") config)]
        (is (nil? v))))
    (testing "commit msg body line has meets maximum characters, for multi element"
      (let [v (common/validate-commit-msg-body-len (common/split-lines "Body abcde\nAb\nBody abcde\nAb\nBody abcde") config)]
        (is (nil? v))))))


(deftest add-string-if-key-empty-test
  (testing "text empty, and collection value not empty"
    (let [v (common/add-string-if-key-empty "" "Added text." :data {:data "non empty"})]
      (is (string? v))
      (is (= "" v))))
  (testing "text empty, and collection value is not defined so would be empty"
    (let [v (common/add-string-if-key-empty "" "Added text." :other {:data "non empty"})]
      (is (string? v))
      (is (= "Added text." v))))
  (testing "text empty, and collection value is nil so is empty"
    (let [v (common/add-string-if-key-empty "" "Added text." :data {:data nil})]
      (is (string? v))
      (is (= "Added text." v))))
  (testing "text not empty, and collection value not empty"
    (let [v (common/add-string-if-key-empty "Original text." "Added text." :data {:data "non empty"})]
      (is (string? v))
      (is (= "Original text." v))))
  (testing "text not empty, and collection value is not defined so would be empty"
    (let [v (common/add-string-if-key-empty "Original text." "Added text." :other {:data "non empty"})]
      (is (string? v))
      (is (= "Original text.  Added text." v))))
  (testing "text not empty, and collection value is nil so is empty"
    (let [v (common/add-string-if-key-empty "Original text." "Added text." :data {:data nil})]
      (is (string? v))
      (is (= "Original text.  Added text." v)))))


(deftest validate-commit-msg-title-scope-type
  (testing "invalid - no type"
    (let [v (common/validate-commit-msg-title-scope-type "(proj): add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= (:reason v) "Bad form on title.  Could not identify type, scope, or description."))
      (is (false? (contains? v :wip)))
      (is (false? (contains? v :type)))
      (is (false? (contains? v :scope)))
      (is (false? (contains? v :breaking)))
      (is (false? (contains? v :title-descr)))
      (is (seq? (:locations v)))
      (is (= 1 (count (:locations v))))
      (is (= 0 (first (:locations v))))))
  (testing "invalid - no scope"
    (let [v (common/validate-commit-msg-title-scope-type "feat(): add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= (:reason v) "Bad form on title.  Could not identify type, scope, or description."))
      (is (false? (contains? v :wip)))
      (is (false? (contains? v :type)))
      (is (false? (contains? v :scope)))
      (is (false? (contains? v :breaking)))
      (is (false? (contains? v :title-descr)))
      (is (seq? (:locations v)))
      (is (= 1 (count (:locations v))))
      (is (= 0 (first (:locations v))))))
  (testing "invalid - no description"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj):")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= (:reason v) "Bad form on title.  Could not identify description."))
      (is (false? (contains? v :wip)))
      (is (false? (contains? v :type)))
      (is (false? (contains? v :scope)))
      (is (false? (contains? v :breaking)))
      (is (false? (contains? v :title-descr)))
      (is (seq? (:locations v)))
      (is (= 1 (count (:locations v))))
      (is (= 0 (first (:locations v))))))
  (testing "valid without exclamation mark or numbers"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj): add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (boolean? (:wip v)))
      (is (false? (:wip v)))
      (is (string? (:type v)))
      (is (= (:type v) "feat"))
      (is (string? (:scope v)))
      (is (= (:scope v) "proj"))
      (is (boolean? (:breaking v)))
      (is (false? (:breaking v)))
      (is (string? (:title-descr v)))
      (is (= (:title-descr v) "add cool new feature"))))
  (testing "valid without exclamation mark and with numbers"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj00): add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (boolean? (:wip v)))
      (is (false? (:wip v)))
      (is (string? (:type v)))
      (is (= (:type v) "feat"))
      (is (string? (:scope v)))
      (is (= (:scope v) "proj00"))
      (is (boolean? (:breaking v)))
      (is (false? (:breaking v)))
      (is (string? (:title-descr v)))
      (is (= (:title-descr v) "add cool new feature"))))
  (testing "valid with exclamation mark"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj)!: add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (boolean? (:wip v)))
      (is (false? (:wip v)))
      (is (string? (:type v)))
      (is (= (:type v) "feat"))
      (is (string? (:scope v)))
      (is (= (:scope v) "proj"))
      (is (boolean? (:breaking v)))
      (is (true? (:breaking v)))
      (is (string? (:title-descr v)))
      (is (= (:title-descr v) "add cool new feature"))))
  (testing "valid with exclamation mark"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj)!: add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (boolean? (:wip v)))
      (is (false? (:wip v)))
      (is (string? (:type v)))
      (is (= (:type v) "feat"))
      (is (string? (:scope v)))
      (is (= (:scope v) "proj"))
      (is (boolean? (:breaking v)))
      (is (true? (:breaking v)))
      (is (string? (:title-descr v)))
      (is (= (:title-descr v) "add cool new feature"))))
  (testing "valid with wip indicator"
    (let [v (common/validate-commit-msg-title-scope-type "~feat(proj): add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (boolean? (:wip v)))
      (is (true? (:wip v)))
      (is (string? (:type v)))
      (is (= (:type v) "feat"))
      (is (string? (:scope v)))
      (is (= (:scope v) "proj"))
      (is (boolean? (:breaking v)))
      (is (false? (:breaking v)))
      (is (string? (:title-descr v)))
      (is (= (:title-descr v) "add cool new feature"))))
  (testing "invalid: separator with no following/ending sub-scope"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj.)!: add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= (:reason v) "Bad form on title.  Could not identify type, scope, or description."))
      (is (false? (contains? v :wip)))
      (is (false? (contains? v :type)))
      (is (false? (contains? v :scope)))
      (is (false? (contains? v :breaking)))
      (is (false? (contains? v :title-descr)))
      (is (seq? (:locations v)))
      (is (= 1 (count (:locations v))))
      (is (= 0 (first (:locations v))))))
  (testing "invalid: separators with no sub-scope"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj.alpha..charlie)!: add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (string? (:reason v)))
      (is (= (:reason v) "Bad form on title.  Could not identify type, scope, or description."))
      (is (false? (contains? v :wip)))
      (is (false? (contains? v :type)))
      (is (false? (contains? v :scope)))
      (is (false? (contains? v :breaking)))
      (is (false? (contains? v :title-descr)))
      (is (seq? (:locations v)))
      (is (= 1 (count (:locations v))))
      (is (= 0 (first (:locations v))))))
  (testing "valid with sub-scope"
    (let [v (common/validate-commit-msg-title-scope-type "feat(proj.alpha.b.charlie)!: add cool new feature")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (boolean? (:wip v)))
      (is (false? (:wip v)))
      (is (string? (:type v)))
      (is (= (:type v) "feat"))
      (is (string? (:scope v)))
      (is (= (:scope v) "proj.alpha.b.charlie"))
      (is (boolean? (:breaking v)))
      (is (true? (:breaking v)))
      (is (string? (:title-descr v)))
      (is (= (:title-descr v) "add cool new feature")))))


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


(deftest validate-commit-msg-test
  (let [config {:commit-msg {:length {:title-line {:min 12        ;; 'ab(cd): efgh' = 12 chars
                                                   :max 20}
                                      :body-line {:min 2
                                                  :max 10}}}}
        config-more-chars {:commit-msg {:length {:title-line {:min 12
                                                              :max 60}
                                                 :body-line {:min 2
                                                             :max 10}}}
                           :project {:scope "top"
                                     :scope-alias "t"
                                     :types ["ci"]
                                     :projects [{:scope "alpha" :scope-alias "a" :types ["feat"]} {:scope "bravo" :scope-alias "b" :artifacts [{:scope "sub"}]} {:scope "charlie" :scope-alias "c"} {:scope "delta" :scope-alias "d"}]}}]
    ;; test commit-msg overall: isn't empty (nil or empty string)
    (testing "invalid: commit msg is nil"
      (let [v (common/validate-commit-msg nil config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot be empty." (:reason v)))
        (is (false? (contains? v :locations)))))
    (testing "invalid: commit msg is empty string"
      (let [v (common/validate-commit-msg "" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot be empty." (:reason v)))
        (is (false? (contains? v :locations)))))
    ;; test commit-msg overall: doesn't contain tab characters
    (testing "invalid: commit msg contains tab on one line"
      (let [v (common/validate-commit-msg "ab(cd): efgh\n\ntabhere	x\nLine 3 ok" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot contain tab characters." (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 2 (first (:locations v))))))
    (testing "invalid: commit msg contains tab on three lines"
      (let [v (common/validate-commit-msg "ab(cd): efgh\n\ntabhere	x\nLine 3 ok\ntabhere	x\nLine 5 ok\ntabhere	x" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot contain tab characters." (:reason v)))
        (is (seq? (:locations v)))
        (is (= 3 (count (:locations v))))
        (is (= 2 (first (:locations v))))
        (is (= 4 (nth (:locations v) 1)))
        (is (= 6 (nth (:locations v) 2)))))
    ;; test commit-msg title: min/max characters
    (testing "invalid: commit msg title line has too few characters"
      (let [v (common/validate-commit-msg "ab(cd): efg\n\nAbcdef" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must be at least " (:min (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid: commit msg title line has too many characters"
      (let [v (common/validate-commit-msg "ab(cd): efghijklmnopq\n\nAbcdef" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must not contain more than " (:max (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    ;; test commit-msg body: min/max characters
    (testing "invalid: commit msg body line has too few characters, for single element"
      (let [v (common/validate-commit-msg "ab(cd): efgh\n\nA" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid: commit msg body line has too few characters, for multi element"
      (let [v (common/validate-commit-msg "ab(cd): efgh\n\nA\nAbcd\nA\nAbc\nA" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 3 (count (:locations v))))
        (is (= 0 (first (:locations v))))
        (is (= 2 (nth (:locations v) 1)))
        (is (= 4 (nth (:locations v) 2)))))
    (testing "invalid: commit msg body line has too many characters, for single element"
      (let [v (common/validate-commit-msg "ab(cd): efgh\n\nAbcdefghijk" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid: commit msg body line has too many characters, for multi element"
      (let [v (common/validate-commit-msg "ab(cd): efgh\n\nAbcdefghijk\nAbc\nAbcdefghijk\nAbcdefghijklmnop\nAbc" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 3 (count (:locations v))))
        (is (= 0 (first (:locations v))))
        (is (= 2 (nth (:locations v) 1)))
        (is (= 3 (nth (:locations v) 2)))))
    ;; test type/scope, !, descr: format, length, retrieval
    (testing "invalid - no type"
      (let [v (common/validate-commit-msg "(proj): add cool new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Bad form on title.  Could not identify type, scope, or description."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid - no scope"
      (let [v (common/validate-commit-msg "feat(): add cool new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Bad form on title.  Could not identify type, scope, or description."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid - no description"
      (let [v (common/validate-commit-msg "ab(cd):" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Commit message title line must be at least 12 characters."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    ;; scope path not found
    (testing "invalid - top-level scope not found in config"
      (let [v (common/validate-commit-msg "ab(zulu): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Definition for scope or scope-alias in title line of 'zulu' at query path of '[:project]' not found in config."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid - second-level scope not found in config"
      (let [v (common/validate-commit-msg "ab(top.zulu): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Definition for scope or scope-alias in title line of 'zulu' at query path of '[:project [:artifacts :projects]]' not found in config."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    ;; type not found
    (testing "invalid - type not found in config for top-level project"
      (let [v (common/validate-commit-msg "ab(top): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Definition in title line of type 'ab' for scope 'top' at query path of '[:project]' not found in config."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid - type not found in config for second-level project"
      (let [v (common/validate-commit-msg "ab(top.alpha): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (:reason v) "Definition in title line of type 'ab' for scope 'top.alpha' at query path of '[:project :projects 0]' not found in config."))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    ;; valid
    (testing "valid - top-level project, non-breaking change"
      (let [v (common/validate-commit-msg "ci(top): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (boolean? (:wip v)))
        (is (false? (:wip v)))
        (is (= 1 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= 1 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= "ci" (:type v)))
        (is (boolean? (:breaking v)))
        (is (false? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))
    (testing "valid - top-level project, wip indicator"
      (let [v (common/validate-commit-msg "~ci(top): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (boolean? (:wip v)))
        (is (true? (:wip v)))
        (is (= 1 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= 1 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= "ci" (:type v)))
        (is (boolean? (:breaking v)))
        (is (false? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))
    (testing "valid - top-level project, breaking change indicated in title"
      (let [v (common/validate-commit-msg "ci(top)!: a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (boolean? (:wip v)))
        (is (false? (:wip v)))
        (is (= 1 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= 1 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= "ci" (:type v)))
        (is (boolean? (:breaking v)))
        (is (true? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))
    (testing "valid - top-level project, breaking change indicated in body"
      (let [v (common/validate-commit-msg "ci(top): a new feature\n\nMore info\nBREAKING CHANGE: this causes a breaking change." (assoc-in config-more-chars [:commit-msg :length :body-line :max] 60))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (boolean? (:wip v)))
        (is (false? (:wip v)))
        (is (= 1 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= 1 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= "ci" (:type v)))
        (is (boolean? (:breaking v)))
        (is (true? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))
    (testing "valid - top-level project, non-breaking change"
      (let [v (common/validate-commit-msg "feat(top.alpha): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (= 2 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= "alpha" (nth (:scope-path v) 1)))
        (is (= 3 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= :projects (nth (:json-path v) 1)))
        (is (= 0 (nth (:json-path v) 2)))
        (is (= "feat" (:type v)))
        (is (boolean? (:breaking v)))
        (is (false? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))
    (testing "valid - top-level project, breaking change indicated in title"
      (let [v (common/validate-commit-msg "feat(top.alpha)!: a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (boolean? (:wip v)))
        (is (false? (:wip v)))
        (is (= 2 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= "alpha" (nth (:scope-path v) 1)))
        (is (= 3 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= :projects (nth (:json-path v) 1)))
        (is (= 0 (nth (:json-path v) 2)))
        (is (= "feat" (:type v)))
        (is (boolean? (:breaking v)))
        (is (true? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))
    (testing "valid - top-level project, breaking change indicated in body"
      (let [v (common/validate-commit-msg "feat(top.alpha): a new feature\n\nMore info\nBREAKING CHANGE: this causes a breaking change." (assoc-in config-more-chars [:commit-msg :length :body-line :max] 60))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (boolean? (:wip v)))
        (is (false? (:wip v)))
        (is (= 2 (count (:scope-path v))))
        (is (= "top" (nth (:scope-path v) 0)))
        (is (= "alpha" (nth (:scope-path v) 1)))
        (is (= 3 (count (:json-path v))))
        (is (= :project (nth (:json-path v) 0)))
        (is (= :projects (nth (:json-path v) 1)))
        (is (= 0 (nth (:json-path v) 2)))
        (is (= "feat" (:type v)))
        (is (boolean? (:breaking v)))
        (is (true? (:breaking v)))
        (is (false? (contains? v :reason)))
        (is (false? (contains? v :locations)))))))


(deftest create-scope-string-from-vector-test
  (testing "empty"
    (let [v (common/create-scope-string-from-vector [])]
      (is (= "" v))))
  (testing "1 element"
    (let [v (common/create-scope-string-from-vector ["alpha"])]
      (is (= "alpha" v))))
  (testing "2 elements"
    (let [v (common/create-scope-string-from-vector ["alpha" "bravo"])]
      (is (= "alpha.bravo" v))))
  (testing "3 elements"
    (let [v (common/create-scope-string-from-vector ["alpha" "bravo" "charlie"])]
      (is (= "alpha.bravo.charlie" v)))))


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
                            :projects [
                                       {:scope "bravo"
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

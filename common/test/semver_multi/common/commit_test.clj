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


(ns semver-multi.common.commit-test
  (:require [clojure.test               :refer [deftest is testing]]
            [clojure.string             :as str]
            [babashka.classpath         :as cp]
            [babashka.process           :refer [shell]]
            [semver-multi.common.system :as system]
            [semver-multi.common.string :as cstr]
            [semver-multi.common.commit :as commit]))


(cp/add-classpath "./")



;; from https://clojuredocs.org/clojure.core/with-out-str#example-590664dde4b01f4add58fe9f
(defmacro with-out-str-data-map
  "Performs the form in the `body` and returns a map result with key 'result' set to the return value and 'str' set to the string output, if any.  Equivalent to 'with-out-str' but returns a map result."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r# ~@body]
         {:result r#
          :str    (str s#)}))))


;;
;; section: re-write (re-format) a commit message
;;

(deftest format-commit-msg-all-test
  (testing "empty string"
    (let [v (commit/format-commit-msg-all "")]
      (is (= "" v))
      (is (string? v))))
  (testing "replace all lines with comments with empty strings"
    (let [v (commit/format-commit-msg-all "#Comment0\nLine1\n#Comment2\nLine3\n #Comment4\nLine5\n#  Comment 6\nLine7")]
      (is (= "Line1\n\nLine3\n\nLine5\n\nLine7" v))
      (is (string? v))))
  (testing "for a line with spaces only, remove all spaces"
    (let [v (commit/format-commit-msg-all "Line1\n \nLine3\n   \nLine5")]
      (is (= "Line1\n\nLine3\n\nLine5" v))
      (is (string? v))))
  (testing "replace two or more consecutive newlines with a single newline"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (commit/format-commit-msg-all "Line1\nLine2\n\nLine3\n\nLine4\nLine5\nLine6\n\n\nLine7\n\n\n\n\n\nLine8")]
      (is (= "Line1\n\nLine2\n\nLine3\n\nLine4\nLine5\nLine6\n\nLine7\n\nLine8" v))
      (is (string? v))))
  (testing "remove spaces at end of lines (without removing spaces at beginning of lines)"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (commit/format-commit-msg-all "Line1\nLine2  \n  Line3  \nLine4\n Line5 ")]
      (is (= "Line1\n\nLine2\n  Line3\nLine4\n Line5" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE: <msg>' formatted correctly"
    (let [v (commit/format-commit-msg-all "BREAKING CHANGE: a change")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' with spaces"
    (let [v (commit/format-commit-msg-all "  BREAKING CHANGE  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' lowercase"
    (let [v (commit/format-commit-msg-all "  breaking change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' mixed case"
    (let [v (commit/format-commit-msg-all "  BreaKing chANge  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' separated with underscore"
    (let [v (commit/format-commit-msg-all "  breaking_change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' separated with dash"
    (let [v (commit/format-commit-msg-all "  breaking-change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' misspeled braking"
    (let [v (commit/format-commit-msg-all "  braking change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "convert to 'BREAKING CHANGE:<msg>' misspeled braeking"
    (let [v (commit/format-commit-msg-all "  braeking change  :   a change  ")]
      (is (= "BREAKING CHANGE: a change" v))
      (is (string? v))))
  (testing "remove leading/trailing spaces"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (commit/format-commit-msg-all "  Line1\nTest\nLine2  ")]
      (is (= "Line1\n\nTest\nLine2" v))
      (is (string? v))))
  (testing "remove leading/trailing newlines"
    ;; "Line1\n\nLine2" because of regex for "<title>\n\n<body>"
    (let [v (commit/format-commit-msg-all "\nLine1\nTest\nLine2\n")]
      (is (= "Line1\n\nTest\nLine2" v))
      (is (string? v)))))


(deftest format-commit-msg-first-line-test
  (testing "empty string"
    (let [v (commit/format-commit-msg-first-line "")]
      (is (= "" v))
      (is (string? v))))
  (testing "without exclamation mark"
    (let [v (commit/format-commit-msg-first-line "    feat  (  client  )    :      add super neat feature   ")]
      (is (= "feat(client): add super neat feature" v))
      (is (string? v))))
  (testing "with exclamation mark"
    (let [v (commit/format-commit-msg-first-line "    feat  (  client  )  !  :      add super neat feature   ")]
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
    (let [v (commit/format-commit-msg nil)]
      (is (= "" v))
      (is (string? v))))
  (testing "empty string"
    (let [v (commit/format-commit-msg "")]
      (is (= "" v))
      (is (string? v))))
  (testing "one line"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   ")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one line and newline"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one line and multiple newlines"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n\n\n")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one line and comment"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n#Comment here")]
      (is (= "feat(client)!: add super neat feature" v))
      (is (string? v))))
  (testing "one newline then body"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \nBody starts here")]
      (is (= "feat(client)!: add super neat feature\n\nBody starts here" v))
      (is (string? v))))
  (testing "two newlines then body"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n\nBody starts here")]
      (is (= "feat(client)!: add super neat feature\n\nBody starts here" v))
      (is (string? v))))
  (testing "three newlines then body"
    (let [v (commit/format-commit-msg "    feat  (  client  )  !  :      add super neat feature   \n\n\nBody starts here")]
      (is (= "feat(client)!: add super neat feature\n\nBody starts here" v))
      (is (string? v))))
  (testing "long commit message"
    (let [v (commit/format-commit-msg long-commit-msg)]
      (is (= long-commit-msg-expected v))
      (is (string? v)))))


;;
;; section: generate a commit message success or error message 
;;


(deftest generate-commit-msg-offending-line-header-test
  (testing "lines is empty vector"
    (let [v (commit/generate-commit-msg-offending-line-header [] nil)]
      (is (vector? v))
      (is (= 0 (count v)))))
  (testing "lines is empty string"
    (let [v (commit/generate-commit-msg-offending-line-header [""] nil)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "" (first v)))))
  (testing "lines-num is nil (no offending line)"
    (let [v (commit/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] nil)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num is empty sequence (no offending line)"
    (let [v (commit/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] '())]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num indicates first line"
    (let [v (commit/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] '(0))]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))
      (is (= "\"   offending line(s) # (1) in red **************\"" (nth v 2)))))
  (testing "lines-num indicates second line"
    (let [v (commit/generate-commit-msg-offending-line-header ["Line 1" "Line 2"] '(1))]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))
      (is (= "\"   offending line(s) # (2) in red **************\"" (nth v 2)))))
  (testing "lines-num indicates first and third lines, but not second line"
    (let [v (commit/generate-commit-msg-offending-line-header ["Line 1" "Line 2" "Line 3"] '(0 2))]
      (is (vector? v))
      (is (= 4 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))
      (is (= "Line 3" (nth v 2)))
      (is (= "\"   offending line(s) # (1 3) in red **************\"" (nth v 3))))))


(deftest generate-commit-msg-offending-line-msg-highlight-test
  (testing "lines is empty vector"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight [] nil)]
      (is (vector? v))
      (is (= 0 (count v)))))
  (testing "lines is empty string"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight [""] nil)]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "" (first v)))))
  (testing "lines-num is nil (no offending line)"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] nil)]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num is empty (no offending line)"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] '())]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num indicates first line"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] '(0))]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "\\e[1m\\e[31mLine 1\\033[0m\\e[0m" (first v)))
      (is (= "Line 2" (nth v 1)))))
  (testing "lines-num indicates second line"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2"] '(1))]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "Line 1" (first v)))
      (is (= "\\e[1m\\e[31mLine 2\\033[0m\\e[0m" (nth v 1)))))
  (testing "lines-num indicates first and third lines"
    (let [v (commit/generate-commit-msg-offending-line-msg-highlight ["Line 1" "Line 2" "Line 3"] '(0 2))]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "\\e[1m\\e[31mLine 1\\033[0m\\e[0m" (nth v 0)))
      (is (= "Line 2" (nth v 1)))
      (is (= "\\e[1m\\e[31mLine 3\\033[0m\\e[0m" (nth v 2))))))


(deftest generate-commit-msg-test
  (testing "lines is empty string"
    (let [v (commit/generate-commit-msg "")]
      (is (seq? v))
      (is (= 7 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (= "echo -e " (nth v 3)))
      (is (true? (str/includes? (nth v 5) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num is nil (no offending line)"
    (let [v (commit/generate-commit-msg "Line 1\nLine 2" nil)]
      (is (seq? v))
      (is (= 8 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (= "echo -e Line 1" (nth v 3)))
      (is (= "echo -e Line 2" (nth v 4)))
      (is (true? (str/includes? (nth v 6) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num is empty (no offending line)"
    (let [v (commit/generate-commit-msg "Line 1\nLine 2" '())]
      (is (seq? v))
      (is (= 8 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (= "echo -e Line 1" (nth v 3)))
      (is (= "echo -e Line 2" (nth v 4)))
      (is (true? (str/includes? (nth v 6) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num indicates first line"
    (let [v (commit/generate-commit-msg "Line 1\nLine 2" '(0))]
      (is (seq? v))
      (is (= 9 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (true? (str/includes? (nth v 2) "echo -e \"   offending line(s) # (1) in red")))
      (is (= "echo -e \\e[1m\\e[31mLine 1\\033[0m\\e[0m" (nth v 4)))
      (is (= "echo -e Line 2" (nth v 5)))
      (is (true? (str/includes? (nth v 7) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num indicates second line"
    (let [v (commit/generate-commit-msg "Line 1\nLine 2" '(1))]
      (is (seq? v))
      (is (= 9 (count v)))
      (is (true? (str/includes? (nth v 1) "echo -e \"BEGIN - COMMIT MESSAGE")))
      (is (true? (str/includes? (nth v 2) "echo -e \"   offending line(s) # (2) in red")))
      (is (= "echo -e Line 1" (nth v 4)))
      (is (= "echo -e \\e[1m\\e[31mLine 2\\033[0m\\e[0m" (nth v 5)))
      (is (true? (str/includes? (nth v 7) "echo -e \"END - COMMIT MESSAGE")))))
  (testing "line-num indicates first and third lines"
    (let [v (commit/generate-commit-msg "Line 1\nLine 2\nLine 3" '(0 2))]
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
    (let [v (commit/generate-commit-err-msg "A title." "An error message.")]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED A title.\"" (first v)))
      (is (= "echo -e \"\\e[1m\\e[31mCommit failed reason: An error message.\\033[0m\\e[0m\"" (nth v 1))))))


(deftest handle-err-test
  (with-redefs [system/exit-now! (fn [x] x)]
    (testing "title and error msg"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-err "The title" "The err message"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and one-line commit msg w/o line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-err "The title" "The err message" "Commit msg line 1"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e Commit msg line 1\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and multi-line commit msg w/o line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-err "The title" "The err message" "Commit msg line 1\nAnd line 2\nAnd line 3"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e Commit msg line 1\necho -e And line 2\necho -e And line 3\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and one-line commit msg w/ line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-err "The title" "The err message" "Commit msg line 1" [0]))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (1) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e \\e[1m\\e[31mCommit msg line 1\\033[0m\\e[0m\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "title, error msg, and multi-line commit msg w/ line num"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-err "The title" "The err message" "Commit msg line 1\nAnd line 2\nAnd line 3" [1]))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED The title\"\necho -e \"\\e[1m\\e[31mCommit failed reason: The err message\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (2) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e Commit msg line 1\necho -e \\e[1m\\e[31mAnd line 2\\033[0m\\e[0m\necho -e And line 3\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))))


(deftest generate-commit-warn-msg-test
  (testing "title and err-msg"
    (let [v (commit/generate-commit-warn-msg "A title." "A warning message.")]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (= "echo -e \"\\e[1m\\e[33mCOMMIT WARNING A title.\"" (first v)))
      (is (= "echo -e \"\\e[1m\\e[33mCommit proceeding with warning: A warning message.\\033[0m\\e[0m\"" (nth v 1))))))


(deftest handle-warn-proceed-test
  (with-redefs [system/exit-now! (fn [x] x)]
    (testing "normal usage"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-warn-proceed "The title" "The message"))]
          (is (= 0 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[33mCOMMIT WARNING The title\"\necho -e \"\\e[1m\\e[33mCommit proceeding with warning: The message\\033[0m\\e[0m\"\n" (:str v))))))))


(deftest handle-ok
  (with-redefs [system/exit-now! (fn [x] x)]
    (testing "normal usage"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (commit/handle-ok "The title"))]
          (is (= 0 (:result v)))
          (is (= "echo -e \"\\e[0m\\e[1mCommit ok, per The title\"\n" (:str v))))))))


(deftest create-validate-commit-msg-err-test
  (testing "reason without locations"
    (let [v (commit/create-validate-commit-msg-err "Reason error occurred")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (string? (:reason v)))
      (is (false? (contains? v :locations)))))
  (testing "reason with locations"
    (let [v (commit/create-validate-commit-msg-err "Reason error occurred" '(3 7 9))]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (string? (:reason v)))
      (is (= 3 (count (:locations v))))
      (is (= 3 (first (:locations v))))
      (is (= 7 (nth (:locations v) 1)))
      (is (= 9 (nth (:locations v) 2))))))


;;
;; section: validate a commit message
;;

(deftest validate-commit-msg-title-len-test
  (let [config {:commit-msg {:length {:title-line {:min 12    ;; 'ab(cd): efgh' = 12 chars
                                                   :max 20}
                                      :body-line {:min 2
                                                  :max 10}}}}]
    (testing "commit msg title line has too few characters"
      (let [v (commit/validate-commit-msg-title-len "ab(cd): efg" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must be at least " (:min (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg title line meets minimum characters"
      (let [v (commit/validate-commit-msg-title-len "ab(cd): efgh" config)]
        (is (nil? v))))
    (testing "commit msg title line has too many characters"
      (let [v (commit/validate-commit-msg-title-len "ab(cd): efghijklmnopq" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must not contain more than " (:max (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg title line meets maximum characters"
      (let [v (commit/validate-commit-msg-title-len "ab(cd): efghijklmnop" config)]
        (is (nil? v))))))


(deftest validate-commit-msg-body-len-test
  (let [config {:commit-msg {:length {:title-line {:min 3
                                                   :max 8}
                                      :body-line {:min 2
                                                  :max 10}}}}]
    (testing "commit msg body is an empty sequence"
      (let [v (commit/validate-commit-msg-body-len [] config)]
        (is (nil? v))))
    (testing "commit msg body line has too few characters, for single element"
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "L") config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg body line has too few characters, for multi element"
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "L\nHello\nA\nAnother line\nX") config)]
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
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "Li") config)]
        (is (nil? v))))
    (testing "commit msg body line has meets minimum characters, for multi element"
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "Li\nAb\nAbcdef\nAb\nAb") config)]
        (is (nil? v))))
    (testing "commit msg body line has too many characters, for single element"
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "Body abcdef") config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "commit msg body line has too many characters, for multi element"
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "Body abcdef\nAbcd\nBody abcdef\nBody abcdef\nAbcd\nBody abcdef") config)]
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
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "Body abcde") config)]
        (is (nil? v))))
    (testing "commit msg body line has meets maximum characters, for multi element"
      (let [v (commit/validate-commit-msg-body-len (cstr/split-lines "Body abcde\nAb\nBody abcde\nAb\nBody abcde") config)]
        (is (nil? v))))))


(deftest validate-commit-msg-title-scope-type
  (testing "invalid - no type"
    (let [v (commit/validate-commit-msg-title-scope-type "(proj): add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(): add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj):")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj): add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj00): add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj)!: add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj)!: add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "~feat(proj): add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj.)!: add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj.alpha..charlie)!: add cool new feature")]
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
    (let [v (commit/validate-commit-msg-title-scope-type "feat(proj.alpha.b.charlie)!: add cool new feature")]
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
      (let [v (commit/validate-commit-msg nil config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot be empty." (:reason v)))
        (is (false? (contains? v :locations)))))
    (testing "invalid: commit msg is empty string"
      (let [v (commit/validate-commit-msg "" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot be empty." (:reason v)))
        (is (false? (contains? v :locations)))))
    ;; test commit-msg overall: doesn't contain tab characters
    (testing "invalid: commit msg contains tab on one line"
      (let [v (commit/validate-commit-msg "ab(cd): efgh\n\ntabhere	x\nLine 3 ok" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= "Commit message cannot contain tab characters." (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 2 (first (:locations v))))))
    (testing "invalid: commit msg contains tab on three lines"
      (let [v (commit/validate-commit-msg "ab(cd): efgh\n\ntabhere	x\nLine 3 ok\ntabhere	x\nLine 5 ok\ntabhere	x" config)]
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
      (let [v (commit/validate-commit-msg "ab(cd): efg\n\nAbcdef" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message title line must be at least " (:min (:title-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid: commit msg title line has too many characters"
      (let [v (commit/validate-commit-msg "ab(cd): efghijklmnopq\n\nAbcdef" config)]
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
      (let [v (commit/validate-commit-msg "ab(cd): efgh\n\nA" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid: commit msg body line has too few characters, for multi element"
      (let [v (commit/validate-commit-msg "ab(cd): efgh\n\nA\nAbcd\nA\nAbc\nA" config)]
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
      (let [v (commit/validate-commit-msg "ab(cd): efgh\n\nAbcdefghijk" config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (= (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") (:reason v)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid: commit msg body line has too many characters, for multi element"
      (let [v (commit/validate-commit-msg "ab(cd): efgh\n\nAbcdefghijk\nAbc\nAbcdefghijk\nAbcdefghijklmnop\nAbc" config)]
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
      (let [v (commit/validate-commit-msg "(proj): add cool new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "feat(): add cool new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "ab(cd):" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "ab(zulu): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (= "zulu" (:scope-or-alias v)))
        (is (= [:project] (:query-path v)))
        (is (false? (contains? v :wip)))
        (is (false? (contains? v :type)))
        (is (false? (contains? v :scope)))
        (is (false? (contains? v :breaking)))
        (is (false? (contains? v :title-descr)))
        (is (seq? (:locations v)))
        (is (= 1 (count (:locations v))))
        (is (= 0 (first (:locations v))))))
    (testing "invalid - second-level scope not found in config"
      (let [v (commit/validate-commit-msg "ab(top.zulu): a new feature" config-more-chars)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (= "zulu" (:scope-or-alias v)))
        (is (= [:project [:artifacts :projects]] (:query-path v)))
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
      (let [v (commit/validate-commit-msg "ab(top): a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "ab(top.alpha): a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "ci(top): a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "~ci(top): a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "ci(top)!: a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "ci(top): a new feature\n\nMore info\nBREAKING CHANGE: this causes a breaking change." (assoc-in config-more-chars [:commit-msg :length :body-line :max] 60))]
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
      (let [v (commit/validate-commit-msg "feat(top.alpha): a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "feat(top.alpha)!: a new feature" config-more-chars)]
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
      (let [v (commit/validate-commit-msg "feat(top.alpha): a new feature\n\nMore info\nBREAKING CHANGE: this causes a breaking change." (assoc-in config-more-chars [:commit-msg :length :body-line :max] 60))]
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

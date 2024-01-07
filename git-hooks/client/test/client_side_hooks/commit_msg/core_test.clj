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
;;	  Project site:  https://github.com/kineticfire-labs/semver-multi


(ns client-side-hooks.commit-msg.core-test
  (:require [clojure.test                      :refer [deftest is testing]]
            [babashka.classpath                :as cp]
            [babashka.process                  :refer [shell]]
            [client-side-hooks.commit-msg.core :as cm]
            [common.core                       :as common]))

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


(defn get-temp-dir
  "Creates a test directory if it doesn't exist and returns a string path to the directory.  The path does NOT end with a slash."
  []
  (let [path "gen/test"]
    (.mkdirs (java.io.File. path))
    path))



;; todo: notes
;; - move client/resources/COMMIT-MSG-EXAMPLE to resources/test/data/... and probably rename/reuse as a test file?
;; - test 'go'?
;; - test 'uber'?


;; todo: testing plans
;; * args
;;    * none
;;    * more than 1 arg
;; * config file
;;    * can't find it / open it
;;    * doesn't parse
;;    * invalid
;;    * disabled
;;       - verify file written
;; - commit edit msg file
;;    * can't find it / open it
;;    * bad:
;;       * tab, line lengths...
;;       * scope/type
;;    - good (verify files written)
;;       - one-line
;;       - multi-line
;;       - reformatting of newlines, comments, etc.
;;    - write file
;;       - err writing
;;       * success (covered by earlier checks)



;; todo
(deftest perform-check-test
  (with-redefs [common/exit-now! (fn [x] x)]
    
    ;; args
    (testing "args: is empty"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check [] "resources/test/data/project-small.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Exactly one argument required.  Usage:  commit-msg <path to git edit message>\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "args: has two values"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["a" "b"] "resources/test/data/project-small.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Exactly one argument required.  Usage:  commit-msg <path to git edit message>\\033[0m\\e[0m\"\n" (:str v))))))


    ;; config file
    (testing "config file: can't open file"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_good-one-line"] "resources/test/data/doesnt-exist.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error reading config file. File 'resources/test/data/doesnt-exist.json' not found. resources/test/data/doesnt-exist.json (No such file or directory)\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "config file: parse fails"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_good-one-line"] "resources/test/data/project-parse-fail.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error reading config file. JSON parse error when reading file 'resources/test/data/project-parse-fail.def.json'.\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "config file: invalid"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_good-one-line"] "resources/test/data/project-invalid.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error validating config file at resources/test/data/project-invalid.def.json. Project required property 'scope' at property 'name' of 'simple-lib' and path '[:config :project]' must be a string.\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "config file: disabled"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_good-one-line"] "resources/test/data/project-disabled.def.json"))]
          (is (= 0 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[33mCOMMIT WARNING by local commit-msg hook.\"\necho -e \"\\e[1m\\e[33mCommit proceeding with warning: Commit message enforcement disabled.\\033[0m\\e[0m\"\n" (:str v))))))
    
    ;; commit message
    (testing "commit message: can't open file"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_doesnt-exist"] "resources/test/data/project-large.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error reading git commit edit message file 'resources/test/data/COMMIT_EDITMSG_doesnt-exist'. File 'resources/test/data/COMMIT_EDITMSG_doesnt-exist' not found. resources/test/data/COMMIT_EDITMSG_doesnt-exist (No such file or directory)\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "commit message: invalid format - line length of title line"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_bad-format"] "resources/test/data/project-large.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Commit message invalid 'resources/test/data/COMMIT_EDITMSG_bad-format'. Commit message title line must not contain more than 50 characters.\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (1) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e \\e[1m\\e[31mfeat(p.client.app)!: add super neat feature but cause a commit message reject by adding a title line description that is too long\\033[0m\\e[0m\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))
    (testing "commit message: invalid format - scope/type"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_bad-scope-type"] "resources/test/data/project-large.def.json"))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Commit message invalid 'resources/test/data/COMMIT_EDITMSG_bad-scope-type'. Definition in title line of type 'zulu' for scope 'p.client.app' at query path of '[:project :projects 0 :artifacts 0]' not found in config.\\033[0m\\e[0m\"\necho -e \"\\e[34m**********************************************\"\necho -e \"BEGIN - COMMIT MESSAGE ***********************\"\necho -e \"   offending line(s) # (1) in red **************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\necho -e \\e[1m\\e[31mzulu(p.client.app)!: add super neat feature\\033[0m\\e[0m\necho -e \"\\e[34m**********************************************\"\necho -e \"END - COMMIT MESSAGE *************************\"\necho -e \"**********************************************\\033[0m\\e[0m\"\n" (:str v))))))

    ;; todo - continue here
    (comment (testing "commit message: ???"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check ["resources/test/data/COMMIT_EDITMSG_???"] "resources/test/data/project-large.def.json"))]
          (is (= 1 (:result v)))
          (is (= "todo" (:str v)))))))
    
    ;; todo - more?
    ))

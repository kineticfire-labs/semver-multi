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


(ns semver-multi.common.shell-test
  (:require [clojure.test              :refer [deftest is testing]]
            [clojure.string            :as str]
            [babashka.classpath        :as cp]
            [babashka.process          :refer [shell]]
            [clojure.java.io           :as io]
            [semver-multi.common.shell :as cshell])
  (:import (java.io File)))


(cp/add-classpath "./")



(deftest run-shell-command-test
  (testing "string, empty"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "\n" (with-out-str (cshell/run-shell-command ""))))))
  (testing "string, non-empty"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "item1\n" (with-out-str (cshell/run-shell-command "item1"))))))
  (testing "vector, empty"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "" (with-out-str (cshell/run-shell-command []))))))
  (testing "vector, empty string"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "\n" (with-out-str (cshell/run-shell-command [""]))))))
  (testing "vector, one string"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "item1\n" (with-out-str (cshell/run-shell-command ["item1"]))))))
  (testing "vector, two strings"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "item1\nitem2\n" (with-out-str (cshell/run-shell-command ["item1" "item2"])))))))


(deftest apply-display-with-shell-test
  (testing "string input"
    (let [v (cshell/apply-display-with-shell "test line")]
      (is (string? v))
      (is (= "echo -e test line" v))))
  (testing "vector string input"
    (let [v (cshell/apply-display-with-shell ["test line 1" "test line 2" "test line 3"])]
      (is (seq? v))
      (is (= 3 (count v)))
      (is (= "echo -e test line 1" (nth v 0)))
      (is (= "echo -e test line 2" (nth v 1)))
      (is (= "echo -e test line 3" (nth v 2))))))


(deftest apply-display-with-shell-without-newline-test
  (testing "string input"
    (let [v (cshell/apply-display-with-shell-without-newline "test line")]
      (is (string? v))
      (is (= "echo -n -e test line" v)))))


(deftest apply-quotes
  (testing "string input"
    (let [v (cshell/apply-quotes "test line")]
      (is (string? v))
      (is (= "\"test line\"" v))))
  (testing "vector string input"
    (let [v (cshell/apply-quotes ["test line 1" "test line 2" "test line 3"])]
      (is (seq? v))
      (is (= 3 (count v)))
      (is (= "\"test line 1\"" (nth v 0)))
      (is (= "\"test line 2\"" (nth v 1)))
      (is (= "\"test line 3\"" (nth v 2))))))


(deftest generate-shell-newline-characters-test
  (testing "no arg"
    (let [v (cshell/generate-shell-newline-characters)]
      (is (string? v))
      (is (= "\n" v))))
  (testing "arg=1"
    (let [v (cshell/generate-shell-newline-characters 1)]
      (is (string? v))
      (is (= "\n" v))))
  (testing "arg=3"
    (let [v (cshell/generate-shell-newline-characters 3)]
      (is (string? v))
      (is (= "\n\n\n" v)))))
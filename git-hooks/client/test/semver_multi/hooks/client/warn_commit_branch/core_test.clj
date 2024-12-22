;; (c) Copyright 2024-2025 semver-multi Contributors. All rights reserved.
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


(ns semver-multi.hooks.client.warn-commit-branch.core-test
  (:require [clojure.test                                      :refer [deftest is testing]]
            [babashka.classpath                                :as cp]
            [babashka.process                                  :refer [shell]]
            [semver-multi.hooks.client.warn-commit-branch.core :as wcb]
            [semver-multi.common.system                        :as system]))


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


(deftest generate-warn-msg-test
  (let [v (wcb/generate-warn-msg "main")]
    (is (seq? v))
    (is (= 2 (count v)))
    (is (= "echo -e \"\\e[1m\\e[31mWARNING\"" (nth v 0)))
    (is (= "echo -e \"\\e[1m\\e[31mYou are attempting to commit to branch 'main'.\\033[0m\\e[0m\"" (nth v 1)))))


(deftest generate-prompt-msg-test
  (let [v (wcb/generate-prompt-msg "main")]
    (is (string? v))
    (is (= "echo -e \"\\e[0m\\e[1mType 'yes' if you wish to continue the commit to branch 'main'.  Any other input aborts the commit.\\033[0m\\e[0m\"" v))))


(deftest generate-prompt-test
  (let [v (wcb/generate-prompt)]
    (is (string? v))
    (is (= "echo -n -e \">> \"" v))))


(deftest generate-proceed-msg-test
  (let [v (wcb/generate-proceed-msg "main")]
    (is (string? v))
    (is (= "echo -e \"\\e[1m\\e[31mProceeding with the commit to branch 'main'.\\033[0m\\e[0m\"" v))))


(deftest generate-abort-msg-test
  (let [v (wcb/generate-abort-msg "main")]
    (is (string? v))
    (is (= "echo -e \"\\e[1m\\e[31mAborting the commit to branch 'main'.\\033[0m\\e[0m\"" v))))


(deftest proceed-test
 (with-redefs [shell (fn [x] (println x))
               system/exit-now! (fn [x] x)]
   (let [v (with-out-str-data-map (wcb/proceed "main"))]
     (is (= 0 (:result v)))
     (is (= "echo -e \"\\e[1m\\e[31mProceeding with the commit to branch 'main'.\\033[0m\\e[0m\"\n" (:str v))))))


(deftest abort-test
  (with-redefs [shell (fn [x] (println x))
                system/exit-now! (fn [x] x)]
    (let [v (with-out-str-data-map (wcb/abort "main"))]
      (is (= 1 (:result v)))
      (is (= "echo -e \"\\e[1m\\e[31mAborting the commit to branch 'main'.\\033[0m\\e[0m\"\n" (:str v))))))
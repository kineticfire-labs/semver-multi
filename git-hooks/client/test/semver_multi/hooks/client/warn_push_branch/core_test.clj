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


;; KineticFire Labs
;;	  Project site:  https://github.com/kineticfire-labs/semver-multi


(ns semver-multi.hooks.client.warn-push-branch.core-test
  (:require [clojure.test                                      :refer [deftest is testing]]
            [babashka.classpath                                :as cp]
            [babashka.process                                  :refer [shell]]
            [semver-multi.hooks.client.warn-push-branch.core   :as wpb]
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
  (testing "1 branch"
    (let [v (wpb/generate-warn-msg ["alpha"])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (= "echo -e \"\\e[1m\\e[31mWARNING\"" (nth v 0)))
      (is (= "echo -e \"\\e[1m\\e[31mYou are attempting to push to branches [alpha].\\033[0m\\e[0m\"" (nth v 1)))))
  (testing "2 branches"
    (let [v (wpb/generate-warn-msg ["alpha" "bravo"])]
      (is (seq? v))
      (is (= 2 (count v)))
      (is (= "echo -e \"\\e[1m\\e[31mWARNING\"" (nth v 0)))
      (is (= "echo -e \"\\e[1m\\e[31mYou are attempting to push to branches [alpha, bravo].\\033[0m\\e[0m\"" (nth v 1))))))


(deftest generate-prompt-msg-test
  (testing "1 branch"
    (let [v (wpb/generate-prompt-msg ["alpha"])]
      (is (string? v))
      (is (= "echo -e \"\\e[0m\\e[1mType 'yes' if you wish to continue the push to branches [alpha].  Any other input aborts the push.\\033[0m\\e[0m\"" v))))
  (testing "2 branches"
    (let [v (wpb/generate-prompt-msg ["alpha" "bravo"])]
      (is (string? v))
      (is (= "echo -e \"\\e[0m\\e[1mType 'yes' if you wish to continue the push to branches [alpha, bravo].  Any other input aborts the push.\\033[0m\\e[0m\"" v)))))


(deftest generate-prompt-test
  (let [v (wpb/generate-prompt)]
    (is (string? v))
    (is (= "echo -n -e \">> \"" v))))


(deftest generate-proceed-msg-test
  (testing "1 branch"
    (let [v (wpb/generate-proceed-msg ["alpha"])]
      (is (string? v))
      (is (= "echo -e \"\\e[1m\\e[31mProceeding with the push to branches [alpha].\\033[0m\\e[0m\"" v))))
  (testing "2 branches"
    (let [v (wpb/generate-proceed-msg ["alpha" "bravo"])]
      (is (string? v))
      (is (= "echo -e \"\\e[1m\\e[31mProceeding with the push to branches [alpha, bravo].\\033[0m\\e[0m\"" v)))))


(deftest generate-abort-msg-test
  (testing "1 branch"
    (let [v (wpb/generate-abort-msg ["alpha"])]
      (is (string? v))
      (is (= "echo -e \"\\e[1m\\e[31mAborting the push to branches [alpha].\\033[0m\\e[0m\"" v))))
  (testing "2 branches"
    (let [v (wpb/generate-abort-msg ["alpha" "bravo"])]
      (is (string? v))
      (is (= "echo -e \"\\e[1m\\e[31mAborting the push to branches [alpha, bravo].\\033[0m\\e[0m\"" v)))))


(deftest proceed-test
 (with-redefs [shell (fn [x] (println x))
               system/exit-now! (fn [x] x)]
   (testing "1 branch"
     (let [v (with-out-str-data-map (wpb/proceed ["alpha"]))]
       (is (= 0 (:result v)))
       (is (= "echo -e \"\\e[1m\\e[31mProceeding with the push to branches [alpha].\\033[0m\\e[0m\"\n" (:str v)))))
   (testing "2 branches"
     (let [v (with-out-str-data-map (wpb/proceed ["alpha" "bravo"]))]
       (is (= 0 (:result v)))
       (is (= "echo -e \"\\e[1m\\e[31mProceeding with the push to branches [alpha, bravo].\\033[0m\\e[0m\"\n" (:str v)))))))


(deftest abort-test
  (with-redefs [shell (fn [x] (println x))
                system/exit-now! (fn [x] x)]
    (testing "1 branch"
      (let [v (with-out-str-data-map (wpb/abort ["alpha"]))]
        (is (= 1 (:result v)))
        (is (= "echo -e \"\\e[1m\\e[31mAborting the push to branches [alpha].\\033[0m\\e[0m\"\n" (:str v)))))
    (testing "2 branches"
      (let [v (with-out-str-data-map (wpb/abort ["alpha" "bravo"]))]
        (is (= 1 (:result v)))
        (is (= "echo -e \"\\e[1m\\e[31mAborting the push to branches [alpha, bravo].\\033[0m\\e[0m\"\n" (:str v)))))))


(deftest get-affected-branches-test
  (testing "no branches"
    (let [v (wpb/get-affected-branches "")]
      (is (vector? v))
      (is (empty? v))))
  (testing "1 branch"
    (let [v (wpb/get-affected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc")]
      (is (vector? v))
      (is (= 1 (count v)))))
  (testing "2 branches"
    (let [v (wpb/get-affected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc\nrefs/heads/1-test 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/1-test 9591a6519b30e816aaeadec8e30f8731906bd707")]
      (is (vector? v))
      (is (= 2 (count v)))))
  (testing "3 branches"
    (let [v (wpb/get-affected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc\nrefs/heads/1-test 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/1-test 9591a6519b30e816aaeadec8e30f8731906bd707\nrefs/heads/alpha 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/alpha 9591a6519b30e816aaeadec8e30f8731906bd707")]
      (is (vector? v))
      (is (= 3 (count v))))))


(deftest get-affected-protected-branches-test
  (testing "no branches, no protected branches"
    (let [v (wpb/get-affected-protected-branches "" [])]
      (is (vector? v))
      (is (empty? v))))
  (testing "no branches, 1 protected branch"
    (let [v (wpb/get-affected-protected-branches "" ["main"])]
      (is (vector? v))
      (is (empty? v))))
  (testing "no branches, 2 protected branches"
    (let [v (wpb/get-affected-protected-branches "" ["main" "1-test"])]
      (is (vector? v))
      (is (empty? v))))
  ;;
  (testing "1 branch, no protected branches"
    (let [v (wpb/get-affected-protected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc" [])]
      (is (vector? v))
      (is (empty? v))))
  (testing "1 branch, 1 protected branch"
    (let [v (wpb/get-affected-protected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc" ["main"])]
      (is (vector? v))
      (is (= 1 (count v)))))
  (testing "1 branch, 2 protected branches"
    (let [v (wpb/get-affected-protected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc" ["main" "1-test"])]
      (is (vector? v))
      (is (= 1 (count v)))))
  ;;
  (testing "2 branches, no protected branches"
    (let [v (wpb/get-affected-protected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc\nrefs/heads/1-test 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/1-test 9591a6519b30e816aaeadec8e30f8731906bd707" [])]
      (is (vector? v))
      (is (empty? v))))
  (testing "2 branches, 1 protected branch"
    (let [v (wpb/get-affected-protected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc\nrefs/heads/1-test 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/1-test 9591a6519b30e816aaeadec8e30f8731906bd707" ["main"])]
      (is (vector? v))
      (is (= 1 (count v)))))
  (testing "2 branches, 2 protected branches"
    (let [v (wpb/get-affected-protected-branches "refs/heads/main e8f368b2f803c06755e99db5f3722f378f2a31ef refs/heads/main 740273a1fed6fcf86400a0e432b7df0880cb8fcc\nrefs/heads/1-test 4f9f41ca5040df420a52550929fe914c6c47389a refs/heads/1-test 9591a6519b30e816aaeadec8e30f8731906bd707" ["main" "1-test"])]
      (is (vector? v))
      (is (= 2 (count v))))))
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


;; KineticFire Labs: https://labs.kineticfire.com/
;;	   Project site: https://github.com/kineticfire-labs/semver-multi/


(ns semver-multi.common.util-test
  (:require [clojure.test                :refer [deftest is testing]]
            [babashka.classpath          :as cp]
            [kineticfire.collections.set :as kf-set]
            [semver-multi.common.util    :as util]))


(cp/add-classpath "./")




(deftest do-if-condition-true-test
  ;; nil
  (testing "condition nil = false so always return true, where fn would have returned true"
    (is (true? (util/do-if-condition-true nil #(boolean true)))))
  (testing "condition nil = false so always return true, where fn would have returned false"
    (is (true? (util/do-if-condition-true nil #(boolean false)))))
  ;; false
  (testing "condition boolean false so always return true, where fn would have returned true"
    (is (true? (util/do-if-condition-true false #(boolean true)))))
  (testing "condition boolean false so always return true, where fn would have returned false"
    (is (true? (util/do-if-condition-true false #(boolean false)))))
  ;; true
  (testing "condition true so return result of fn, where fn returns true"
    (is (true? (util/do-if-condition-true true #(boolean true)))))
  (testing "condition true so return result of fn, where fn returns false"
    (is (false? (util/do-if-condition-true true #(boolean false)))))
  ;; neither nil nor false so true
  (testing "condition neither nil nor false so true so return result of fn, where fn returns true"
    (is (true? (util/do-if-condition-true 1 #(boolean true)))))
  (testing "condition neither nil nor false so true so return result of fn, where fn returns false"
    (is (false? (util/do-if-condition-true 1 #(boolean false)))))
  ;; example usage with "does map contain key"
  (testing "condition evals to false so always return true, where fn would have returned true"
    (is (true? (util/do-if-condition-true (contains? {} :a) #(boolean true)))))
  (testing "condition evals to false so always return true, where fn would have returned false"
    (is (true? (util/do-if-condition-true (contains? {} :a) #(boolean false)))))
  (testing "condition evals to true so return result of fn, where fn returns true"
    (is (true? (util/do-if-condition-true (contains? {:a 1} :a) #(boolean true)))))
  (testing "condition evals to true so return result of fn, where fn returns false"
    (is (false? (util/do-if-condition-true (contains? {:a 1} :a) #(boolean false))))))


(deftest do-if-condition-false-test
  ;; nil
  (testing "condition nil = false, where fn returns true"
    (is (true? (util/do-if-condition-false nil #(boolean true)))))
  (testing "condition nil = false, where fn returns false"
    (is (false? (util/do-if-condition-false nil #(boolean false)))))
  ;; false
  (testing "condition false, where fn returns true"
    (is (true? (util/do-if-condition-false false #(boolean true)))))
  (testing "condition false, where fn returnS false"
    (is (false? (util/do-if-condition-false false #(boolean false)))))
  ;; true
  (testing "condition true, where fn would have returned true"
    (is (true? (util/do-if-condition-false true #(boolean true)))))
  (testing "condition true, where fn returnS would have returned false"
    (is (true? (util/do-if-condition-false true #(boolean false)))))
  ;; neither nil nor false so true
  (testing "condition neither nil nor false, where fn would have returned true"
    (is (true? (util/do-if-condition-false 1 #(boolean true)))))
  (testing "condition neither nil nor false, where fn returnS would have returned false"
    (is (true? (util/do-if-condition-false 1 #(boolean false)))))
  ;; example usage with "does map contain key"
  (testing "condition evals to false so return result of fn, where fn returns true"
    (is (true? (util/do-if-condition-false (contains? {} :a) #(boolean true)))))
  (testing "condition evals to false so return result of fn, where fn returns false"
    (is (false? (util/do-if-condition-false (contains? {} :a) #(boolean false)))))
  (testing "condition evals to true so always return true, where fn returns true"
    (is (true? (util/do-if-condition-false (contains? {:a 1} :a) #(boolean true)))))
  (testing "condition evals to true so always return true, where fn returns false"
    (is (true? (util/do-if-condition-false (contains? {:a 1} :a) #(boolean false))))))


(defn perform-valid-string?-test
  [nil-ok min max fn-expected str]
  (let [v (util/valid-string? nil-ok min max str)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-string?-test
  (testing "invalid: nil w/ nil not ok"
    (perform-valid-string?-test false 1 5 false? nil))
  (testing "valid: nil w/ nil ok"
    (perform-valid-string?-test true 1 5 true? nil))
  (testing "invalid: not string, input number"
    (perform-valid-string?-test false 1 5 false? 5))
  (testing "invalid: not string, input vector of strings"
    (perform-valid-string?-test false 1 5 false? ["alpha" "bravo"]))
  (testing "invalid: less than min (empty string)"
    (perform-valid-string?-test false 1 5 false? ""))
  (testing "invalid: greater than max"
    (perform-valid-string?-test false 1 5 false? "abcdef"))
  (testing "valid: equal to min"
    (perform-valid-string?-test false 1 5 true? "a"))
  (testing "valid: greater than min"
    (perform-valid-string?-test false 1 5 true? "ab"))
  (testing "valid: equal to max"
    (perform-valid-string?-test false 1 5 true? "abcde"))
  (testing "valid: less than max"
    (perform-valid-string?-test false 1 5 true? "abcd")))


(defn perform-valid-integer?-test
  [nil-ok min max fn-expected int]
  (let [v (util/valid-integer? nil-ok min max int)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-integer?-test
  (testing "invalid: nil w/ nil not ok"
    (perform-valid-integer?-test false 1 5 false? nil))
  (testing "valid: nil w/ nil ok"
    (perform-valid-integer?-test true 1 5 true? nil))
  (testing "invalid: not integer, input string"
    (perform-valid-integer?-test false 1 5 false? "3"))
  (testing "invalid: not integer, input float"
    (perform-valid-integer?-test false 1 5 false? 0.0))
  (testing "invalid: not integer, input collection of integers"
    (perform-valid-integer?-test false 1 5 false? [1 2 3]))
  (testing "invalid: less than min"
    (perform-valid-integer?-test false 1 5 false? 0))
  (testing "invalid: greater than max"
    (perform-valid-integer?-test false 1 5 false? 6))
  (testing "valid: equal to min"
    (perform-valid-integer?-test false 1 5 true? 1))
  (testing "valid: greater than min"
    (perform-valid-integer?-test false 1 5 true? 2))
  (testing "valid: equal to max"
    (perform-valid-integer?-test false 1 5 true? 5))
  (testing "valid: less than max"
    (perform-valid-integer?-test false 1 5 true? 4)))


(defn perform-valid-coll?-test
  [duplicates-ok min max fn col fn-expected]
  (let [v (util/valid-coll? duplicates-ok min max fn col)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-coll?-test
  (let [fn (partial util/valid-string? false 1 4)]
    (testing "invalid: collection is nil"
      (perform-valid-coll?-test false 1 5 fn nil false?))
    (testing "invalid: not a collection"
      (perform-valid-coll?-test false 1 5 fn "hello" false?))
    (testing "invalid: less than min (empty)"
      (perform-valid-coll?-test false 1 5 fn [] false?))
    (testing "invalid: greater than max"
      (perform-valid-coll?-test false 1 5 fn ["a" "b" "c" "d" "e" "f"] false?))
    (testing "invalid: duplicates and duplicates not ok"
      (perform-valid-coll?-test false 1 5 fn ["a" "b" "a" "d"] false?))
    (testing "valid: w/ duplicates and duplicates ok"
      (perform-valid-coll?-test true 1 5 fn ["a" "b" "a" "d"] true?))
    (testing "valid: no duplicates"
      (perform-valid-coll?-test false 1 5 fn ["a" "b" "c" "d"] true?))
    ;;
    ;; applying fn
    (testing "invalid: per fn since an element isn't a string"
      (perform-valid-coll?-test false 1 5 fn ["a" 2 "c" "d"] false?))))


(defn perform-valid-map-entry?-test
  [key-path required nil-ok fn map fn-expected]
  (let [v (util/valid-map-entry? key-path required nil-ok fn map)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest valid-map-entry?-test
  (let [fn-string (partial util/valid-string? false 1 5)
        fn-coll (partial util/valid-coll? false 1 5 fn-string)]
    ;;
    ;; required and nil
    (testing "invalid: map nil so key-path not found w/ required true"
      (perform-valid-map-entry?-test [:a :b] true false fn-string nil false?))
    (testing "valid: map nil so key-path not found w/ required false"
      (perform-valid-map-entry?-test [:a :b] false false fn-string nil true?))
    (testing "invalid: key-path not found w/ required true"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:c 1}} false?))
    (testing "valid: key-path not found w/ required false"
      (perform-valid-map-entry?-test [:a :b] false false fn-string {:a {:c 1}} true?))
    (testing "invalid: nil w/ nil not ok"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b nil}} false?))
    (testing "valid: nil w/ nil ok"
      (perform-valid-map-entry?-test [:a :b] true true fn-string {:a {:b nil}} true?))
    ;;
    ;; applying fn:  scalar, using 'valid-string?' function
    (testing "invalid: not string, input number"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b 1}} false?))
    (testing "invalid: not string, input vector of strings"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b ["alpha" "bravo"]}} false?))
    (testing "invalid: string length less than min (empty string)"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b ""}} false?))
    (testing "invalid: string length greater than max"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b "abcdef"}} false?))
    (testing "valid: string length equal to min"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b "a"}} true?))
    (testing "valid: string length equal to max"
      (perform-valid-map-entry?-test [:a :b] true false fn-string {:a {:b "abcde"}} true?))
    ;;
    ;; applying fn: coll, using 'valid-coll?' w/ 'valid-string?'
    (testing "invalid: coll, exceed max elements"
      (perform-valid-map-entry?-test [:a :b] true false fn-coll {:a {:b ["a" "b" "c" "d" "e" "f"]}} false?))
    (testing "valid: coll"
      (perform-valid-map-entry?-test [:a :b] true false fn-coll {:a {:b ["a" "b" "c"]}} true?))))


(defn perform-valid-string-as-keyword?-test
  [nil-ok str expected]
  (let [v (util/valid-string-as-keyword? nil-ok str)]
    (is (boolean? v))
    (is (= v expected))))


(deftest valid-string-as-keyword?-test
  (testing "valid: nil ok"
    (perform-valid-string-as-keyword?-test true nil true))
  (testing "valid: nil but not ok"
    (perform-valid-string-as-keyword?-test false nil false))
  (testing "invalid: one-digit integer"
    (perform-valid-string-as-keyword?-test false "1" false))
  (testing "invalid: two-digit integer"
    (perform-valid-string-as-keyword?-test false "12" false))
  (testing "invalid: leading dash"
    (perform-valid-string-as-keyword?-test false "-abc" false))
  (testing "invalid: leading underscore"
    (perform-valid-string-as-keyword?-test false "-abc" false))
  (testing "invalid: has space"
    (perform-valid-string-as-keyword?-test false "abc def" false))
  (testing "invalid: has colon"
    (perform-valid-string-as-keyword?-test false "abc:def" false))
  (testing "invalid: has slash"
    (perform-valid-string-as-keyword?-test false "abc/def" false))
  (testing "valid: single char"
    (perform-valid-string-as-keyword?-test false "a" true))
  (testing "valid: multi char"
    (perform-valid-string-as-keyword?-test false "abc" true)))


(defn perform-intersection-vec
  [vec1 vec2 expected-vec]
  (let [actual-vec (util/intersection-vec vec1 vec2)]
    (is (vector? actual-vec))
    (is (empty? (kf-set/symmetric-difference (set expected-vec) (set actual-vec))))))


(deftest intersection-vec-test
  (testing "empty vecs"
    (perform-intersection-vec [] [] []))
  (testing "vec1 populated, vec2 empty"
    (perform-intersection-vec [1 2 3] [] []))
  (testing "vec1 empty, vec2 populated"
    (perform-intersection-vec [] [1 2 3] []))
  (testing "vecs populated, but no overlap"
    (perform-intersection-vec [1 2 3] [4 5 6] []))
  (testing "vecs populated, 1 overlap"
    (perform-intersection-vec [1 2 3] [4 2 6] [2]))
  (testing "vecs populated, 2 overlap"
    (perform-intersection-vec [1 2 3] [4 2 3] [3 2])))


(defn perform-is-semantic-version-release?
  [version result]
  (let [v (util/is-semantic-version-release? version)]
    (is (boolean? v))
    (is (= v result))))


(deftest is-semantic-version-release?-test
  (testing "valid: all 0s"
    (perform-is-semantic-version-release? "0.0.0" true))
  (testing "valid: with 0s for minor and patch"
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
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


;; KineticFire Labs: https://labs.kineticfire.com
;;	   Project site:  https://github.com/kineticfire-labs/semver-multi


(ns semver-multi.common.util-test
  (:require [clojure.test             :refer [deftest is testing]]
            [clojure.set              :as set]
            [babashka.classpath       :as cp]
            [semver-multi.common.util :as util]))


(cp/add-classpath "./")


(defn symmetric-difference-of-sets [set1 set2]
  (set/union (set/difference set1 set2) (set/difference set2 set1)))


(deftest do-on-success-test
  (testing "one arg: success"
    (let [v (util/do-on-success #(update % :val inc) {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 2 (:val v)))))
  (testing "one arg: fail"
    (let [v (util/do-on-success #(update % :val inc) {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "two args: success"
    (let [v (util/do-on-success #(assoc %2 :val (+ (:val %2) %1)) 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 6 (:val v)))))
  (testing "two args: fail"
    (let [v (util/do-on-success #(assoc %2 :val (+ (:val %2) %1)) 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "three args: success"
    (let [v (util/do-on-success #(assoc %3 :val (+ (:val %3) %1 %2)) 2 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 8 (:val v)))))
  (testing "three args: fail"
    (let [v (util/do-on-success #(assoc %3 :val (+ (:val %3) %1 %2)) 2 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v)))))
  (testing "four args: success"
    (let [v (util/do-on-success #(assoc %4 :val (+ (:val %4) %1 %2 %3)) 1 2 5 {:success true :val 1})]
      (is (map? v))
      (is (true? (:success v)))
      (is (= 9 (:val v)))))
  (testing "four args: fail"
    (let [v (util/do-on-success #(assoc %4 :val (+ (:val %4) %1 %2 %3)) 1 2 5 {:success false :val 1})]
      (is (map? v))
      (is (false? (:success v)))
      (is (= 1 (:val v))))))


(defn perform-contains-value-test
  [col search fn-expected]
  (let [v (util/contains-value? col search)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest contains-value-test
  (testing "vector: contains"
    (perform-contains-value-test [1 2 3] 2 true?))
  (testing "vector: does not contain"
    (perform-contains-value-test [1 2 3] 0 false?))
  (testing "vector: empty"
    (perform-contains-value-test [] 2 false?))
  (testing "list: contains"
    (perform-contains-value-test '(1 2 3) 2 true?))
  (testing "list: does not contain"
    (perform-contains-value-test '(1 2 3) 0 false?))
  (testing "list: empty"
    (perform-contains-value-test '() 2 false?))
  (testing "set: contains"
    (perform-contains-value-test #{1 2 3} 2 true?))
  (testing "set: does not contain"
    (perform-contains-value-test #{1 2 3} 0 false?))
  (testing "set: empty"
    (perform-contains-value-test #{} 2 false?)))


(deftest find-duplicates-test
   (testing "no duplicates: empty vector"
    (let [v (util/find-duplicates [])]
       (is (vector? v))
       (is (= 0 (count v)))))
  (testing "no duplicates, integer: populated vector"
    (let [v (util/find-duplicates [1 2 3 10 4])]
       (is (vector? v))
       (is (= 0 (count v)))))
  (testing "no duplicates, string: populated vector"
    (let [v (util/find-duplicates ["alpha" "charlie" "bravo" "foxtrot" "kilo"])]
       (is (vector? v))
       (is (= 0 (count v)))))
  (testing "one duplicate, integer"
    (let [v (util/find-duplicates [1 2 3 10 3])]
       (is (vector? v))
       (is (= 1 (count v)))
       (util/contains-value? v 3)))
  (testing "one duplicate, string"
    (let [v (util/find-duplicates ["alpha" "charlie" "bravo" "alpha" "kilo"])]
       (is (vector? v))
       (is (= 1 (count v)))
       (util/contains-value? v "alpha")))
  (testing "three duplicates, integer"
    (let [v (util/find-duplicates [1 2 3 10 3 1 8 2])]
       (is (vector? v))
       (is (= 3 (count v)))
       (util/contains-value? v 3)
       (util/contains-value? v 1)
       (util/contains-value? v 2)))
  (testing "three duplicates, string"
    (let [v (util/find-duplicates ["alpha" "charlie" "bravo" "alpha" "kilo" "charlie" "bravo"])]
       (is (vector? v))
       (is (= 3 (count v)))
       (util/contains-value? v "alpha")
       (util/contains-value? v "charlie")
       (util/contains-value? v "bravo"))))


(defn perform-duplicates?-test
  [col fn-expected]
  (let [v (util/duplicates? col)]
    (is (boolean? v))
    (is (fn-expected v))))


(deftest duplicates?-test
  (testing "no duplicates: empty vector"
    (perform-duplicates?-test [] false?))
  (testing "no duplicates, integer: populated vector"
    (perform-duplicates?-test [1 2 3 10 4] false?))
  (testing "no duplicates, string: populated vector"
    (perform-duplicates?-test ["alpha" "charlie" "bravo" "foxtrot" "kilo"] false?))
  (testing "one duplicate, integer"
    (perform-duplicates?-test [1 2 3 10 3] true?))
  (testing "one duplicate, string"
    (perform-duplicates?-test ["alpha" "charlie" "bravo" "alpha" "kilo"] true?))
  (testing "three duplicates, integer"
    (perform-duplicates?-test [1 2 3 10 3 1 8 2] true?))
  (testing "three duplicates, string"
    (perform-duplicates?-test ["alpha" "charlie" "bravo" "alpha" "kilo" "charlie" "bravo"] true?)))


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


(defn perform-symmetric-difference-of-sets-test
  [set1 set2 expected]
  (let [v (util/symmetric-difference-of-sets set1 set2)]
    (is (set? v))
    (is (= v expected))))


(deftest symmetric-difference-of-sets-test
  (testing "empty sets"
    (perform-symmetric-difference-of-sets-test #{} #{} #{}))
  (testing "set1 empty, set2 not empty"
    (perform-symmetric-difference-of-sets-test #{1} #{} #{1}))
  (testing "set1 not empty, set2 empty"
    (perform-symmetric-difference-of-sets-test #{} #{1} #{1}))
  (testing "no diff, 1 element"
    (perform-symmetric-difference-of-sets-test #{1} #{1} #{}))
  (testing "no diff, multiple elements"
    (perform-symmetric-difference-of-sets-test #{1 3 5 7} #{7 1 5 3} #{}))
  (testing "diff, 1 element each"
    (perform-symmetric-difference-of-sets-test #{1} #{2} #{1 2}))
  (testing "diff, 2 elements each"
    (perform-symmetric-difference-of-sets-test #{1 2} #{3 4} #{1 2 3 4}))
  (testing "diff, 2 elements each with 2 in common"
    (perform-symmetric-difference-of-sets-test #{1 7 2 8} #{8 3 4 7} #{1 2 3 4})))


(defn perform-intersection-vec
  [vec1 vec2 expected-vec]
  (let [actual-vec (util/intersection-vec vec1 vec2)]
    (is (vector? actual-vec))
    (is (empty? (symmetric-difference-of-sets (set expected-vec) (set actual-vec))))))


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


(defn perform-assoc-in-m-ks-test
  [m ks v expected]
  (let [result (util/assoc-in m ks v)]
    (is (map? result))
    (is (= result expected))))


(defn perform-assoc-in-m-ks-v-coll-test
  [m ks-v-coll expected]
  (let [result (util/assoc-in m ks-v-coll)]
    (is (map? result))
    (is (= result expected))))


(deftest assoc-in-test
  ;;
  ;; m, ks, v
  (testing "m, ks, v: add to existing structure"
    (perform-assoc-in-m-ks-test {:a {:b 1}} [:a :c] 2 {:a {:b 1 :c 2}}))
  (testing "m, ks, v: need to create structure"
    (perform-assoc-in-m-ks-test {:a {:b 1}} [:a :c :d] 2 {:a {:b 1 :c {:d 2}}}))
  ;;
  ;; m, ks-v-coll
  (testing "m, ks-v-coll: one item, add to existing structure"
    (perform-assoc-in-m-ks-v-coll-test {:a {:b 1}} [ [[:a :c] 2] ] {:a {:b 1 :c 2}}))
  (testing "m, ks-v-coll: two items, add to existing structure and create new structure"
    (perform-assoc-in-m-ks-v-coll-test {:a {:b 1}} [ [[:a :c] 2] [[:a :d :e] 3] ] {:a {:b 1 :c 2 :d {:e 3}}})))


(defn perform-dissoc-in-test
  [m ks expected]
  (let [v (util/dissoc-in m ks)]
    (is (map? v))
    (is (= v expected))))


(deftest dissoc-in-test
  (testing "ks is vector of keywords: key doesn't exist"
    (perform-dissoc-in-test {:a {:b 1}} [:a :c] {:a {:b 1}}))
  (testing "ks is vector of keywords: remove top-level key"
    (perform-dissoc-in-test {:a {:b 1}} [:a] {}))
  (testing "ks is vector of keywords: remove only key at that level"
    (perform-dissoc-in-test {:a {:b 1}} [:a :b] {:a {}}))
  (testing "ks is vector of keywords: remove 1 of 2 keys at that level"
    (perform-dissoc-in-test {:a {:b 1 :c 2}} [:a :c] {:a {:b 1}}))
  (testing "ks is vector of vector of keywords of seq: remove 2 keys"
    (perform-dissoc-in-test {:a {:b 1 :c 2}} [[:a :b] [:a :c]] {:a {}}))
  ;;
  (testing "ks is list of strings: key doesn't exist"
    (perform-dissoc-in-test {"a" {"b" 1}} `("a" "c") {"a" {"b" 1}}))
  (testing "ks is list of strings: remove top-level key"
    (perform-dissoc-in-test {"a" {"b" 1}} `("a") {}))
  (testing "ks is list of strings: remove only key at that level"
    (perform-dissoc-in-test {"a" {"b" 1}} '("a" "b") {"a" {}}))
  (testing "ks is list of strings: remove 1 of 2 keys at that level"
    (perform-dissoc-in-test {"a" {"b" 1 "c" 2}} '("a" "c") {"a" {"b" 1}}))
  (testing "ks is list of list of strings of seq: remove 2 keys"
    (perform-dissoc-in-test {"a" {"b" 1 "c" 2}} (list (list "a" "b") (list "a" "c")) {"a" {}})))



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
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


(ns semver-multi.hooks.client.prevent-rebase.core-test
  (:require [clojure.test                                  :refer [deftest is testing]]
            [babashka.classpath                            :as cp]
            [babashka.process                              :refer [shell]]
            [semver-multi.hooks.client.prevent-rebase.core :as pr]
            [semver-multi.common.system                    :as system]))


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


(deftest generate-err-msg-test
  (let [v (pr/generate-err-msg)]
    (is (seq? v))
    (is (= 2 (count v)))
    (is (= "echo -e \"\\e[1m\\e[31mREBASE REJECTED\"" (nth v 0)))
    (is (= "echo -e \"\\e[1m\\e[31mReason: rebase not allowed becase it destroys commit history\\033[0m\\e[0m\"" (nth v 1)))))


(deftest perform-check-test
  (with-redefs [system/exit-now! (fn [x] x)]
    (testing "attempt to rebase"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (pr/perform-prevent-rebase))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mREBASE REJECTED\"\necho -e \"\\e[1m\\e[31mReason: rebase not allowed becase it destroys commit history\\033[0m\\e[0m\"\n" (:str v))))))))

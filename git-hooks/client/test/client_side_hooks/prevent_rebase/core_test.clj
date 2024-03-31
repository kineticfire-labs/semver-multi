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


(ns client-side-hooks.prevent-rebase.core-test
  (:require [clojure.test                                  :refer [deftest is testing]]
            [babashka.classpath                            :as cp]
            [babashka.process                              :refer [shell]]
            [clojure.java.io                               :as io]
            [client-side-hooks.commit-msg-enforcement.core :as cm]
            [common.core                                   :as common])
  (:import (java.io File)))


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


;; todo put 'shell' redef in top-level
(deftest perform-check-test
  (with-redefs [common/exit-now! (fn [x] x)]
    
    ;; args
    (testing "args: is empty"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check [] (str resources-test-data-dir-string "/" "project-small.def.json")))]
          (is (= 1 (:result v)))
          (is (= "echo -e \"\\e[1m\\e[31mCOMMIT REJECTED by local commit-msg hook.\"\necho -e \"\\e[1m\\e[31mCommit failed reason: Error: exactly one argument required.  Usage:  commit-msg <path to git edit message>\\033[0m\\e[0m\"\n" (:str v))))))
    ))

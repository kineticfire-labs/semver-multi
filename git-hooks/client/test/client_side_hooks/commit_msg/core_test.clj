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
;; - args
;;    - none
;;    - more than 1 arg
;; - config file
;;    - can't find it / open it
;;    - that doesn't parse
;;    - that is invalid
;;    - that is enabled vs disabled
;; - commit edit msg file
;;    - can't find it / open it
;;    - bad:
;;       - tab, line lengths...
;;       - scope/type
;;    - good
;;       - one-line
;;       - multi-line
;;       - reformatting of newlines, comments, etc.
;;    - write file
;;       - err writing
;;       - success


;; todo need for testing:
;; - config file
;;    - that doesn't parse
;;    - that is invalid
;;    - good config file
;;       - that is enabled vs disabled
;;       - simple?
;;       - complex?
;; - commit edit msg file (since this gets re-written, need to copy test file into another dir and use that path)
;;    - bad: tab, line lengths...
;;    - good
;;       - one-line
;;       - multi-line
;;       - needs reformatting of newlines, comments, etc.


;; todo
(deftest perform-check-test
  (with-redefs [common/exit-now! (fn [x] x)]
    (testing "title and error msg"
      (with-redefs [shell (fn [x] (println x))]
        (let [v (with-out-str-data-map (cm/perform-check "todo path-to-commit-edit-msg" "todo config-file"))]
          (is (= 1 (:result v)))
          (is (= "todo" (:str v))))))))
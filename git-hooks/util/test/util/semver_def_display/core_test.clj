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


(ns util.semver-def-display.core-test
  (:require [clojure.test                      :refer [deftest is testing]]
            [babashka.classpath                :as cp]
            [babashka.process                  :refer [shell]]
            [clojure.java.io                   :as io]
            [util.semver-def-display.core      :as d]
            [common.core                       :as common])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/core_test")

(def ^:const resources-test-data-dir-string "resources/test/data")



;; from https://clojuredocs.org/clojure.core/with-out-str#example-590664dde4b01f4add58fe9f
(defmacro with-out-str-data-map
  "Performs the form in the `body` and returns a map result with key 'result' set to the return value and 'str' set to the string output, if any.  Equivalent to 'with-out-str' but returns a map result."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r# ~@body]
         {:result r#
          :str    (str s#)}))))


(defn delete-dir
  "Deletes the file or directory `file`.  If `file` is a directory, then first recursively deletes all contents."
  [^File file]
  (when (.isDirectory file)
    (run! delete-dir (.listFiles file)))
  (io/delete-file file))


(defn setup-temp-dir
  "Sets up the temporary directory for the tests in this file.  Creates the directory if it does not exists, recursively deleting the directory first if it does exist."
  []
  (let [temp-dir (File. temp-dir-string)]
    (when (.exists temp-dir)
      (delete-dir temp-dir))
    (.mkdirs temp-dir)))


;; Configures temporary directory for tests
(setup-temp-dir)


(defn copy-file
  "Copies the file identified by path string `source-path-string` to destination file identified by the path string `dest-path-string`."
  [source-path-string dest-path-string]
  (io/copy (io/file source-path-string) (io/file dest-path-string)))



(deftest handle-ok-test
  (with-redefs [common/exit-now! (fn [x] x)]
    (testing "exit"
      (is (= 0 (d/handle-ok))))))


(deftest handle-err-test
  (with-redefs [common/exit-now! (fn [x] x)
                shell (fn [x] (println x))]
    (testing "with message"
      (let [v (with-out-str-data-map (d/handle-err "The err msg."))]
        (is (= 1 (:result v)))
        (is (= "echo -e The err msg.\n" (:str v)))))))


(deftest handle-warn-test
  (with-redefs [shell (fn [x] (println x))]
    (testing "with message"
      (is (= "echo -e The warn msg.\n" (with-out-str (d/handle-warn "The warn msg.")))))))


(deftest process-options-f
  (testing "after arg flag '-f', args doesn't contain file path"
    (let [v (d/process-options-f {} [] ["-f"])]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Flag '-f' must be followed by a config file path." (:reason v)))))
  (testing "duplicate definition"
    (let [v (d/process-options-f {} [:config-file] ["-f" "path/to/project.def.json"])]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Duplicate definition of config file." (:reason v)))))
  (testing "success"
    (let [v (d/process-options-f {:test "hello"} [] ["-f" "path/to/project.def.json" "x"])]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "hello" (:test (:response v))))
      (is (= "path/to/project.def.json" (:config-file (:response v))))
      (is (= 1 (count (:defined v))))
      (is (= :config-file (first (:defined v))))
      (is (= 1 (count (:args v))))
      (is (= "x" (first (:args v)))))))


(deftest process-options-default-test
  (testing "duplicate definition"
    (let [v (d/process-options-default {} [:alias-scope-path] ["project.client"])]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Duplicate definition of alias scope path." (:reason v)))))
  (testing "success"
    (let [v (d/process-options-default {:test "hello"} [] ["project.client" "x"])]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "hello" (:test (:response v))))
      (is (= "project.client" (:alias-scope-path (:response v))))
      (is (= 1 (count (:defined v))))
      (is (= :alias-scope-path (first (:defined v))))
      (is (= 1 (count (:args v))))
      (is (= "x" (first (:args v)))))))


(deftest process-options-test
  (testing "err: too many CLI args"
    (let [v (d/process-options ["-f" "path/to/project.def.json" "project.client" "x"] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Invalid options format. Zero to two arguments accepted. Usage:  semver-def-display <optional -f config file path> <optional scope path>" (:reason v)))))
  (testing "err: after arg flag '-f', args doesn't contain file path"
    (let [v (d/process-options ["-f"] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Invalid options format. Flag '-f' must be followed by a config file path. Usage:  semver-def-display <optional -f config file path> <optional scope path>" (:reason v)))))
  ;; note: can't do duplicate definition of config file, because will err out first on number of args
  (testing "err: duplicate definition of alias scope path"
    (let [v (d/process-options ["project.client" "a.b.c"] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "Invalid options format. Duplicate definition of alias scope path. Usage:  semver-def-display <optional -f config file path> <optional scope path>" (:reason v)))
      (is (false? (contains? v :alias-scope-path)))
      (is (false? (contains? v :config-file)))))
  (testing "success: no args, using defaults"
    (let [v (d/process-options [] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "default/path/to/project.def.json" (:config-file v)))
      (is (false? (contains? v :alias-scope-path)))))
  (testing "success: specify alias scope path"
    (let [v (d/process-options ["project.client"] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "default/path/to/project.def.json" (:config-file v)))
      (is (= "project.client" (:alias-scope-path v)))))
  (testing "success: specify config file"
    (let [v (d/process-options ["-f" "path/to/project.def.json"] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "path/to/project.def.json" (:config-file v)))
      (is (false? (contains? v :alias-scope-path)))))
  (testing "success: specify config file and alias scope path"
    (let [v (d/process-options ["-f" "path/to/project.def.json" "project.client"] "default/path/to/project.def.json")]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "path/to/project.def.json" (:config-file v)))
      (is (= "project.client" (:alias-scope-path v))))))


(deftest process-alias-scope-path-test
  (testing "err: alias-scope-path not in config"
    (let [v (d/process-alias-scope-path {:test "hello" :alias-scope-path "a.b.c"} {:project {:name "Top Project"
                                                                                             :description "The top project"
                                                                                             :scope "proj"
                                                                                             :scope-alias "p"
                                                                                             :types ["feat", "chore", "refactor"]
                                                                                             :projects [{:name "Subproject A"
                                                                                                         :description "The subproject A"
                                                                                                         :scope "proja"
                                                                                                         :scope-alias "a"
                                                                                                         :types ["feat", "chore", "refactor"]}
                                                                                                        {:name "Subproject B"
                                                                                                         :description "The subproject B"
                                                                                                         :scope "projb"
                                                                                                         :scope-alias "b"
                                                                                                         :types ["feat", "chore", "refactor"]}]
                                                                                             :artifacts [{:name "Artifact Y"
                                                                                                          :description "The artifact Y"
                                                                                                          :scope "arty"
                                                                                                          :scope-alias "y"
                                                                                                          :types ["feat", "chore", "refactor"]}
                                                                                                         {:name "Artifact Z"
                                                                                                          :description "The artifact Z"
                                                                                                          :scope "artz"
                                                                                                          :scope-alias "z"
                                                                                                          :types ["feat", "chore", "refactor"]}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= "hello" (:test v)))
      (is (= "Definition for scope or scope-alias in title line of 'a' at query path of '[:project]' not found in config." (:reason v)))))
  (testing "success:  no alias-scope-path in options"
    (let [v (d/process-alias-scope-path {:test "hello"} {})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "hello" (:test v)))))
  (testing "success: alias-scope-path in config"
    (let [v (d/process-alias-scope-path {:test "hello" :alias-scope-path "proj.a"} {:project {:name "Top Project"
                                                                                              :description "The top project"
                                                                                              :scope "proj"
                                                                                              :scope-alias "p"
                                                                                              :types ["feat", "chore", "refactor"]
                                                                                              :projects [{:name "Subproject A"
                                                                                                          :description "The subproject A"
                                                                                                          :scope "proja"
                                                                                                          :scope-alias "a"
                                                                                                          :types ["feat", "chore", "refactor"]}
                                                                                                         {:name "Subproject B"
                                                                                                          :description "The subproject B"
                                                                                                          :scope "projb"
                                                                                                          :scope-alias "b"
                                                                                                          :types ["feat", "chore", "refactor"]}]
                                                                                              :artifacts [{:name "Artifact Y"
                                                                                                           :description "The artifact Y"
                                                                                                           :scope "arty"
                                                                                                           :scope-alias "y"
                                                                                                           :types ["feat", "chore", "refactor"]}
                                                                                                          {:name "Artifact Z"
                                                                                                           :description "The artifact Z"
                                                                                                           :scope "artz"
                                                                                                           :scope-alias "z"
                                                                                                           :types ["feat", "chore", "refactor"]}]}})]
      (is (map? v))
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= "hello" (:test v)))
      (is (= ["proj" "proja"] (:scope-path v)))
      (is (= [:project :projects 0] (:json-path v))))))



;; todo
(deftest display-output-test
  (testing "stuff"
    (let [v (d/compute-display-config {:project {:name "Top Project"
                                                 :description "The top project"
                                                 :scope "proj"
                                                 :scope-alias "p"
                                                 :types ["feat", "chore", "refactor"]
                                                 :projects [{:name "Subproject A"
                                                             :description "The subproject A"
                                                             :scope "proja"
                                                             :scope-alias "a"
                                                             :types ["feat", "chore", "refactor"]}
                                                            {:name "Subproject B"
                                                             :description "The subproject B"
                                                             :scope "projb"
                                                             :scope-alias "b"
                                                             :types ["feat", "chore", "refactor"]}]
                                                 :artifacts [{:name "Artifact Y"
                                                              :description "The artifact Y"
                                                              :scope "arty"
                                                              :scope-alias "y"
                                                              :types ["feat", "chore", "refactor"]}
                                                             {:name "Artifact Z"
                                                              :description "The artifact Z"
                                                              :scope "artz"
                                                              :scope-alias "z"
                                                              :types ["feat", "chore", "refactor"]}]}} {})]
      (println v)
      (let [y (d/display-output v)]
        (println y))
      )))

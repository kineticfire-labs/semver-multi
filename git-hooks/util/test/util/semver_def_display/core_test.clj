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



(deftest get-highlight-code-test
  (testing "true"
    (is (= common/shell-color-red (d/get-highlight-code true))))
  (testing "false"
    (is (= common/shell-color-white (d/get-highlight-code false)))))


(deftest display-output-test
  (testing "string"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "echo -e \"the item\"\n" (with-out-str (d/display-output "the item"))))))
  (testing "vector, one element"
    (with-redefs [shell (fn [x] (println x))]
      (is (= "echo -e \"item 1\"\n" (with-out-str (d/display-output ["item 1"]))))))
  (testing "vector, two elements"
     (with-redefs [shell (fn [x] (println x))]
       (is (= "echo -e \"item 1\"\necho -e \"item 2\"\n" (with-out-str (d/display-output ["item 1" "item 2"])))))))


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


(deftest compute-display-config-node-header-format-test
  ;; [highlight]
  (testing "highlight true"
    (is (= "\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m" (d/compute-display-config-node-header-format true))))
  (testing "highlight false"
    (is (= "\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" (d/compute-display-config-node-header-format false))))
  ;;
  ;; [type level highlight]
  ;; type = project
  (testing "project, level should always be 0, highlight true"
    (is (= "\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :project 0 true))))
  (testing "project, level should always be 0, highlight false"
    (is (= "\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :project 0 false))))
  ;; type = projects
  (testing "projects, level 0, highlight true"
    (is (= "\\e[1m\\e[31mPROJECTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :projects 0 true))))
  (testing "projects, level 1, highlight true"
    (is (= "\\e[1m\\e[31m    PROJECTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :projects 1 true))))
  (testing "projects, level 2, highlight true"
    (is (= "\\e[1m\\e[31m        PROJECTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :projects 2 true))))
  (testing "projects, level 0, highlight false"
    (is (= "\\e[0m\\e[1mPROJECTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :projects 0 false))))
  (testing "projects, level 1, highlight false"
    (is (= "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :projects 1 false))))
  (testing "projects, level 2, highlight false"
    (is (= "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :projects 2 false))))
    ;; type = artifacts
  (testing "artifacts, level 0, highlight true"
    (is (= "\\e[1m\\e[31mARTIFACTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :artifacts 0 true))))
  (testing "artifacts, level 1, highlight true"
    (is (= "\\e[1m\\e[31m    ARTIFACTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :artifacts 1 true))))
  (testing "artifacts, level 2, highlight true"
    (is (= "\\e[1m\\e[31m        ARTIFACTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :artifacts 2 true))))
  (testing "artifacts, level 0, highlight false"
    (is (= "\\e[0m\\e[1mARTIFACTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :artifacts 0 false))))
  (testing "artifacts, level 1, highlight false"
    (is (= "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :artifacts 1 false))))
  (testing "artifacts, level 2, highlight false"
    (is (= "\\e[0m\\e[1m        ARTIFACTS---------\\033[0m\\e[0m" (d/compute-display-config-node-header-format :artifacts 2 false)))))


(deftest compute-display-config-node-header-test
  ;; empty path
  (testing "empty path"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [] 0 true))))
  ;; path is :project
  (testing "project, level should always be 0, highlight true"
    (is (= ["a" "\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project] 0 true))))
  (testing "project, level should always be 0, highlight false"
    (is (= ["a" "\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project] 0 false))))
  ;; path is :projects
  (testing "first of projects, level 0, highlight true"
    (is (= ["a" "\\e[1m\\e[31mPROJECTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :projects 0] 0 true))))
  (testing "first of projects, level 1, highlight true"
    (is (= ["a" "\\e[1m\\e[31m    PROJECTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :projects 0] 1 true))))
  (testing "first of projects, level 2, highlight true"
    (is (= ["a" "\\e[1m\\e[31m        PROJECTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :projects 0] 2 true))))
  (testing "first of projects, level 0, highlight false"
    (is (= ["a" "\\e[0m\\e[1mPROJECTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :projects 0] 0 false))))
  (testing "first of projects, level 1, highlight false"
    (is (= ["a" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :projects 0] 1 false))))
  (testing "first of projects, level 2, highlight false"
    (is (= ["a" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :projects 0] 2 false))))
  (testing "non-first of projects, level 0, highlight true"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :projects 1] 0 true))))
  (testing "non-first of projects, level 1, highlight true"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :projects 1] 1 true))))
  (testing "non-first of projects, level 0, highlight false"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :projects 1] 0 false))))
  (testing "non-first of projects, level 1, highlight false"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :projects 1] 1 false))))
  ;; path is :artifacts
  (testing "first of artifacts, level 0, highlight true"
    (is (= ["a" "\\e[1m\\e[31mARTIFACTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :artifacts 0] 0 true))))
  (testing "first of artifacts, level 1, highlight true"
    (is (= ["a" "\\e[1m\\e[31m    ARTIFACTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :artifacts 0] 1 true))))
  (testing "first of artifacts, level 2, highlight true"
    (is (= ["a" "\\e[1m\\e[31m        ARTIFACTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :artifacts 0] 2 true))))
  (testing "first of artifacts, level 0, highlight false"
    (is (= ["a" "\\e[0m\\e[1mARTIFACTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :artifacts 0] 0 false))))
  (testing "first of artifacts, level 1, highlight false"
    (is (= ["a" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :artifacts 0] 1 false))))
  (testing "first of artifacts, level 2 highlight false"
    (is (= ["a" "\\e[0m\\e[1m        ARTIFACTS---------\\033[0m\\e[0m"] (d/compute-display-config-node-header ["a"] [:project :artifacts 0] 2 false))))
  (testing "non-first of artifacts, level 0, highlight true"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :artifacts 1] 0 true))))
  (testing "non-first of artifacts, level 1, highlight true"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :artifacts 1] 1 true))))
  (testing "non-first of artifacts, level 0, highlight false"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :artifacts 1] 0 false))))
  (testing "non-first of artifacts, level 1, highlight false"
    (is (= ["a"] (d/compute-display-config-node-header ["a"] [:project :artifacts 1] 1 false)))))


(deftest compute-display-config-node-name-format-test
  (testing "level 0, highlight true"
    (is (= "\\e[1m\\e[31m  item\\033[0m\\e[0m" (d/compute-display-config-node-name-format "item" 0 true))))
  (testing "level 0, highlight false"
    (is (= "\\e[0m\\e[1m  item\\033[0m\\e[0m" (d/compute-display-config-node-name-format "item" 0 false))))
  (testing "level 1, highlight true"
    (is (= "\\e[1m\\e[31m      item\\033[0m\\e[0m" (d/compute-display-config-node-name-format "item" 1 true))))
  (testing "level 1, highlight false"
    (is (= "\\e[0m\\e[1m      item\\033[0m\\e[0m" (d/compute-display-config-node-name-format "item" 1 false))))
  (testing "level 2, highlight true"
    (is (= "\\e[1m\\e[31m          item\\033[0m\\e[0m" (d/compute-display-config-node-name-format "item" 2 true))))
  (testing "level 2, highlight false"
    (is (= "\\e[0m\\e[1m          item\\033[0m\\e[0m" (d/compute-display-config-node-name-format "item" 2 false)))))


(deftest compute-display-config-node-name-test
  (testing "level 0, highlight true"
    (is (= ["a" "\\e[1m\\e[31m  item\\033[0m\\e[0m"] (d/compute-display-config-node-name ["a"] {:name "item"} 0 true))))
  (testing "level 0, highlight false"
    (is (= ["a" "\\e[0m\\e[1m  item\\033[0m\\e[0m"] (d/compute-display-config-node-name ["a"] {:name "item"} 0 false))))
  (testing "level 1, highlight true"
    (is (= ["a" "\\e[1m\\e[31m      item\\033[0m\\e[0m"] (d/compute-display-config-node-name ["a"] {:name "item"} 1 true))))
  (testing "level 1, highlight false"
    (is (= ["a" "\\e[0m\\e[1m      item\\033[0m\\e[0m"] (d/compute-display-config-node-name ["a"] {:name "item"} 1 false))))
  (testing "level 2, highlight true"
    (is (= ["a" "\\e[1m\\e[31m          item\\033[0m\\e[0m"] (d/compute-display-config-node-name ["a"] {:name "item"} 2 true))))
  (testing "level 2, highlight false"
    (is (= ["a" "\\e[0m\\e[1m          item\\033[0m\\e[0m"] (d/compute-display-config-node-name ["a"] {:name "item"} 2 false)))))


(deftest compute-display-config-node-info-format-test
  (testing "level 0"
    (is (= "    item" (d/compute-display-config-node-info-format "item" 0))))
  (testing "level 1"
    (is (= "        item" (d/compute-display-config-node-info-format "item" 1))))
  (testing "level 2"
    (is (= "            item" (d/compute-display-config-node-info-format "item" 2)))))


(deftest add-if-defined-test
  (testing "not defined"
    (is (= ["a"] (d/add-if-defined ["a"] nil [:target] "label" "x" 0))))
  (testing "defined, level 0"
    (is (= ["a" "    xlabel     : item\\033[0m\\e[0m"] (d/add-if-defined ["a"] {:target "item"} [:target] "label" "x" 0))))
  (testing "defined, level 1"
    (is (= ["a" "        xlabel     : item\\033[0m\\e[0m"] (d/add-if-defined ["a"] {:target "item"} [:target] "label" "x" 1))))
  (testing "defined, level 2"
    (is (= ["a" "            xlabel     : item\\033[0m\\e[0m"] (d/add-if-defined ["a"] {:target "item"} [:target] "label" "x" 2)))))


(deftest add-if-defined-comma-sep-test
  (testing "not defined"
    (is (= ["a"] (d/add-if-defined-comma-sep ["a"] nil [:target] "label" "x" 0))))
  (testing "defined, one element, level 0"
    (is (= ["a" "    xlabel     : item1\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1"]} [:target] "label" "x" 0))))
  (testing "defined, two elements, level 0"
    (is (= ["a" "    xlabel     : item1, item2\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1" "item2"]} [:target] "label" "x" 0))))
  (testing "defined, three elements, level 0"
    (is (= ["a" "    xlabel     : item1, item2, item3\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1" "item2" "item3"]} [:target] "label" "x" 0))))
  (testing "defined, one element, level 1"
    (is (= ["a" "        xlabel     : item1\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1"]} [:target] "label" "x" 1))))
  (testing "defined, two elements, level 1"
    (is (= ["a" "        xlabel     : item1, item2\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1" "item2"]} [:target] "label" "x" 1))))
  (testing "defined, three elements, level 1"
    (is (= ["a" "        xlabel     : item1, item2, item3\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1" "item2" "item3"]} [:target] "label" "x" 1))))
  (testing "defined, one element, level 2"
    (is (= ["a" "            xlabel     : item1\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1"]} [:target] "label" "x" 2))))
  (testing "defined, two elements, level 2"
    (is (= ["a" "            xlabel     : item1, item2\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1" "item2"]} [:target] "label" "x" 2))))
  (testing "defined, three elements, level 2"
    (is (= ["a" "            xlabel     : item1, item2, item3\\033[0m\\e[0m"] (d/add-if-defined-comma-sep ["a"] {:target ["item1" "item2" "item3"]} [:target] "label" "x" 2)))))


(deftest compute-display-config-node-info-test
  (testing "not defined"
    (is (= ["a"] (d/compute-display-config-node-info ["a"] nil [] [] [] 0 true))))
  ;; no optionals defined
     ;; single element vectors
  (testing "level 0, w/ highlight, no options, single element info items"
    (is (= ["a" "    \\e[1m\\e[31mname-path : project\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project"] ["proj"] ["p"] 0 true))))
  (testing "level 0, no highlight, no options, single element info items"
    (is (= ["a" "    \\e[0m\\e[1mname-path : project\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project"] ["proj"] ["p"] 0 false))))
  (testing "level 1, w/ highlight, no options, single element info items"
    (is (= ["a" "        \\e[1m\\e[31mname-path : project\\033[0m\\e[0m" "        \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "        \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project"] ["proj"] ["p"] 1 true))))
  (testing "level 1, no highlight, no options, single element info items"
    (is (= ["a" "        \\e[0m\\e[1mname-path : project\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project"] ["proj"] ["p"] 1 false))))
  (testing "level 2, w/ highlight, no options, single element info items"
    (is (= ["a" "            \\e[1m\\e[31mname-path : project\\033[0m\\e[0m" "            \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "            \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project"] ["proj"] ["p"] 2 true))))
  (testing "level 2, no highlight, no options, single element info items"
    (is (= ["a" "            \\e[0m\\e[1mname-path : project\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project"] ["proj"] ["p"] 2 false))))
  ;; no optionals defined
     ;; multi element vectors
  (testing "level 0, w/ highlight, no options, single element info items"
    (is (= ["a" "    \\e[1m\\e[31mname-path : project.alpha bravo\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj.alp.br\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 0 true))))
  (testing "level 0, no highlight, no options, single element info items"
    (is (= ["a" "    \\e[0m\\e[1mname-path : project.alpha bravo\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj.alp.br\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 0 false))))
  (testing "level 1, w/ highlight, no options, single element info items"
    (is (= ["a" "        \\e[1m\\e[31mname-path : project.alpha bravo\\033[0m\\e[0m" "        \\e[1m\\e[31mscope-path: proj.alp.br\\033[0m\\e[0m" "        \\e[1m\\e[31malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 1 true))))
  (testing "level 1, no highlight, no options, single element info items"
    (is (= ["a" "        \\e[0m\\e[1mname-path : project.alpha bravo\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.alp.br\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 1 false))))
  (testing "level 2, w/ highlight, no options, single element info items"
    (is (= ["a" "            \\e[1m\\e[31mname-path : project.alpha bravo\\033[0m\\e[0m" "            \\e[1m\\e[31mscope-path: proj.alp.br\\033[0m\\e[0m" "            \\e[1m\\e[31malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 2 true))))
  (testing "level 2, no highlight, no options, single element info items"
    (is (= ["a" "            \\e[0m\\e[1mname-path : project.alpha bravo\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.alp.br\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty"} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 2 false))))
  ;; optionals defined
     ;; single element vectors
  (testing "level 0, w/ highlight, no options, single element info items"
    (is (= ["a" "    \\e[1m\\e[31mname-path : project\\033[0m\\e[0m" "    \\e[1m\\e[31mincludes  : readme\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme"]} ["project"] ["proj"] ["p"] 0 true))))
  (testing "level 0, no highlight, no options, single element info items"
    (is (= ["a" "    \\e[0m\\e[1mname-path : project\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme"]} ["project"] ["proj"] ["p"] 0 false))))
  (testing "level 1, w/ highlight, no options, single element info items"
    (is (= ["a" "        \\e[1m\\e[31mname-path : project\\033[0m\\e[0m" "        \\e[1m\\e[31mincludes  : readme\\033[0m\\e[0m" "        \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "        \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme"]} ["project"] ["proj"] ["p"] 1 true))))
  (testing "level 1, no highlight, no options, single element info items"
    (is (= ["a" "        \\e[0m\\e[1mname-path : project\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme"]} ["project"] ["proj"] ["p"] 1 false))))
  (testing "level 2, w/ highlight, no options, single element info items"
    (is (= ["a" "            \\e[1m\\e[31mname-path : project\\033[0m\\e[0m" "            \\e[1m\\e[31mincludes  : readme\\033[0m\\e[0m" "            \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "            \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme"]} ["project"] ["proj"] ["p"] 2 true))))
  (testing "level 2, no highlight, no options, single element info items"
    (is (= ["a" "            \\e[0m\\e[1mname-path : project\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme"]} ["project"] ["proj"] ["p"] 2 false))))
  ;; optionals defined
     ;; multi element vectors
  (testing "level 0, w/ highlight, no options, multi element info items"
    (is (= ["a" "    \\e[1m\\e[31mname-path : project.alpha bravo\\033[0m\\e[0m" "    \\e[1m\\e[31mincludes  : readme, user-guide, dev-guide\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj.alp.br\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme", "user-guide", "dev-guide"]} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 0 true))))
  (testing "level 0, no highlight, no options, multi element info items"
    (is (= ["a" "    \\e[0m\\e[1mname-path : project.alpha bravo\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, user-guide, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj.alp.br\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme", "user-guide", "dev-guide"]} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 0 false))))
  (testing "level 1, w/ highlight, no options, multi element info items"
    (is (= ["a" "        \\e[1m\\e[31mname-path : project.alpha bravo\\033[0m\\e[0m" "        \\e[1m\\e[31mincludes  : readme, user-guide, dev-guide\\033[0m\\e[0m" "        \\e[1m\\e[31mscope-path: proj.alp.br\\033[0m\\e[0m" "        \\e[1m\\e[31malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme", "user-guide", "dev-guide"]} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 1 true))))
  (testing "level 1, no highlight, no options, multi element info items"
    (is (= ["a" "        \\e[0m\\e[1mname-path : project.alpha bravo\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme, user-guide, dev-guide\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.alp.br\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme", "user-guide", "dev-guide"]} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 1 false))))
  (testing "level 2, w/ highlight, no options, multi element info items"
    (is (= ["a" "            \\e[1m\\e[31mname-path : project.alpha bravo\\033[0m\\e[0m" "            \\e[1m\\e[31mincludes  : readme, user-guide, dev-guide\\033[0m\\e[0m" "            \\e[1m\\e[31mscope-path: proj.alp.br\\033[0m\\e[0m" "            \\e[1m\\e[31malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme", "user-guide", "dev-guide"]} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 2 true))))
  (testing "level 2, no highlight, no options, multi element info items"
    (is (= ["a" "            \\e[0m\\e[1mname-path : project.alpha bravo\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme, user-guide, dev-guide\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.alp.br\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.a.b\\033[0m\\e[0m"] (d/compute-display-config-node-info ["a"] {:name "not empty" :descrption "the node" :includes ["readme", "user-guide", "dev-guide"]} ["project" "alpha bravo"] ["proj" "alp" "br"] ["p" "a" "b"] 2 false)))))


(deftest get-child-nodes-test
  ;; no child nodes, e.g. no projects or artifacts
  (testing "no child nodes"
    (let [v (d/get-child-nodes {} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 0 (count v)))))
  ;; projects but no artifacts
  (testing "one project, no artifacts"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 0] (:path (nth v 0))))))
  (testing "two projects, no artifacts"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"} {:name "proj2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 1] (:path (nth v 0))))
      (is (= "alpha" (:a (nth v 1))))
      (is (= :projects (:type (nth v 1))))
      (is (= [:project :projects 0] (:path (nth v 1))))))
  ;; artifacts but no projects
  (testing "one artifact, no projects"
    (let [v (d/get-child-nodes {:artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 1 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :artifacts (:type (nth v 0))))
      (is (= [:project :artifacts 0] (:path (nth v 0))))))
  (testing "two artifacts, no projects"
    (let [v (d/get-child-nodes {:artifacts [{:name "art1"} {:name "art2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :artifacts (:type (nth v 0))))
      (is (= [:project :artifacts 1] (:path (nth v 0))))
      (is (= "alpha" (:a (nth v 1))))
      (is (= :artifacts (:type (nth v 1))))
      (is (= [:project :artifacts 0] (:path (nth v 1))))))
  ;; projects and artifacts
  (testing "one project and one artifact"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"}] :artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 0] (:path (nth v 0))))
      (is (= :artifacts (:type (nth v 1))))
      (is (= [:project :artifacts 0] (:path (nth v 1))))))
  (testing "one project and one artifact"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"}] :artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 2 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 0] (:path (nth v 0))))
      (is (= :artifacts (:type (nth v 1))))
      (is (= [:project :artifacts 0] (:path (nth v 1))))))
  (testing "two projects and one artifact"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"} {:name "proj2"}] :artifacts [{:name "art1"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 1] (:path (nth v 0))))
      (is (= :projects (:type (nth v 1))))
      (is (= [:project :projects 0] (:path (nth v 1))))
      (is (= :artifacts (:type (nth v 2))))
      (is (= [:project :artifacts 0] (:path (nth v 2))))))
  (testing "one project and two artifacts"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"}] :artifacts [{:name "art1"} {:name "art2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 3 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 0] (:path (nth v 0))))
      (is (= :artifacts (:type (nth v 1))))
      (is (= [:project :artifacts 1] (:path (nth v 1))))
      (is (= :artifacts (:type (nth v 1))))
      (is (= [:project :artifacts 0] (:path (nth v 2))))))
  (testing "one project and two artifacts"
    (let [v (d/get-child-nodes {:projects [{:name "proj1"} {:name "proj2"}] :artifacts [{:name "art1"} {:name "art2"}]} {:a "alpha"} [:project])]
      (is (vector? v))
      (is (= 4 (count v)))
      (is (= "alpha" (:a (nth v 0))))
      (is (= :projects (:type (nth v 0))))
      (is (= [:project :projects 1] (:path (nth v 0))))
      (is (= :projects (:type (nth v 1))))
      (is (= [:project :projects 0] (:path (nth v 1))))
      (is (= :artifacts (:type (nth v 2))))
      (is (= [:project :artifacts 1] (:path (nth v 2))))
      (is (= :artifacts (:type (nth v 3))))
      (is (= [:project :artifacts 0] (:path (nth v 3)))))))


(deftest build-queue-for-compute-display-config-path-test
  (testing "one item"
    (is (= [[:project]] (d/build-queue-for-compute-display-config-path [:project]))))
  (testing "two items"
    (is (= [[:project] [:project :projects 0]] (d/build-queue-for-compute-display-config-path [:project :projects 0]))))
  (testing "three items"
    (is (= [[:project] [:project :projects 0] [:project :projects 0 :projects 1]] (d/build-queue-for-compute-display-config-path [:project :projects 0 :projects 1])))))


(deftest compute-display-config-path-test
  (testing "json-path is nil"
    (let [v (d/compute-display-config-path {:project {:name "Top Project" :scope "proj" :scope-alias "p"}} nil)]
      (is (= [] (:output v)))
      (is (= [{:path [:project]
              :parent-name-path []
              :parent-scope-path []
              :parent-alias-path []
              :level 0}] (:stack v)))))
  (testing "json-path is top node; no child nodes"
    (let [v (d/compute-display-config-path {:project {:name "Top Project"
                                                      :scope "proj"
                                                      :scope-alias "p"
                                                      :description "The top project."
                                                      :includes ["readme", "dev-guide"]}} [:project])]
      (is (= ["\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m" "\\e[1m\\e[31m  Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mname-path : Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mdescr     : The top project.\\033[0m\\e[0m" "    \\e[1m\\e[31mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (:output v)))
      (is (= [] (:stack v)))))
  (testing "json-path is top node; has child nodes"
    (let [v (d/compute-display-config-path {:project {:name "Top Project"
                                                      :scope "proj"
                                                      :scope-alias "p"
                                                      :description "The top project."
                                                      :includes ["readme", "dev-guide"]
                                                      :projects [{:name "Project1"
                                                                 :scope "proj1"
                                                                 :scope-alias "p1"
                                                                 :description "The project1."
                                                                 :includes ["readme-p1", "dev-guide-p1"]}
                                                                 {:name "Project2"
                                                                  :scope "proj2"
                                                                  :scope-alias "p2"
                                                                  :description "The project2."
                                                                  :includes ["readme-p2", "dev-guide-p2"]}]
                                                      :artifacts [{:name "Artifact1"
                                                                  :scope "art1"
                                                                  :scope-alias "a1"
                                                                  :description "The artifact1."
                                                                  :includes ["readme-a1", "dev-guide-a1"]}
                                                                 {:name "Artifact2"
                                                                  :scope "art2"
                                                                  :scope-alias "a2"
                                                                  :description "The artifact2."
                                                                  :includes ["readme-a2", "dev-guide-a2"]}]}} [:project])]
      (is (= ["\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m" "\\e[1m\\e[31m  Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mname-path : Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mdescr     : The top project.\\033[0m\\e[0m" "    \\e[1m\\e[31mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p\\033[0m\\e[0m"] (:output v)))
      (is (= [{:parent-name-path ["Top Project"], :parent-scope-path ["proj"], :parent-alias-path ["p"], :level 2, :type :projects, :path [:project :projects 1]} {:parent-name-path ["Top Project"], :parent-scope-path ["proj"], :parent-alias-path ["p"], :level 2, :type :projects, :path [:project :projects 0]} {:parent-name-path ["Top Project"], :parent-scope-path ["proj"], :parent-alias-path ["p"], :level 2, :type :artifacts, :path [:project :artifacts 1]} {:parent-name-path ["Top Project"], :parent-scope-path ["proj"], :parent-alias-path ["p"], :level 2, :type :artifacts, :path [:project :artifacts 0]}] (:stack v)))))
      ;; vs code not indenting correctly
      (testing "json-path is child node; no child nodes"
        (let [v (d/compute-display-config-path {:project {:name "Top Project"
                                                          :scope "proj"
                                                          :scope-alias "p"
                                                          :description "The top project."
                                                          :includes ["readme", "dev-guide"]
                                                          :projects [{:name "Project1"
                                                                      :scope "proj1"
                                                                      :scope-alias "p1"
                                                                      :description "The project1."
                                                                      :includes ["readme-p1", "dev-guide-p1"]}
                                                                     {:name "Project2"
                                                                      :scope "proj2"
                                                                      :scope-alias "p2"
                                                                      :description "The project2."
                                                                      :includes ["readme-p2", "dev-guide-p2"]}]
                                                          :artifacts [{:name "Artifact1"
                                                                       :scope "art1"
                                                                       :scope-alias "a1"
                                                                       :description "The artifact1."
                                                                       :includes ["readme-a1", "dev-guide-a1"]}
                                                                      {:name "Artifact2"
                                                                       :scope "art2"
                                                                       :scope-alias "a2"
                                                                       :description "The artifact2."
                                                                       :includes ["readme-a2", "dev-guide-a2"]}]}} [:project :projects 0])]
          (is (= ["\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m" "\\e[1m\\e[31m  Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mname-path : Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mdescr     : The top project.\\033[0m\\e[0m" "    \\e[1m\\e[31mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p\\033[0m\\e[0m" "\\e[1m\\e[31m    PROJECTS---------\\033[0m\\e[0m" "\\e[1m\\e[31m      Project1\\033[0m\\e[0m" "        \\e[1m\\e[31mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[1m\\e[31mdescr     : The project1.\\033[0m\\e[0m" "        \\e[1m\\e[31mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[1m\\e[31mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[1m\\e[31malias-path: p.p1\\033[0m\\e[0m"] (:output v)))
          (is (= [] (:stack v)))))
          (testing "json-path is child node; has child nodes"
            (let [v (d/compute-display-config-path {:project {:name "Top Project"
                                                              :scope "proj"
                                                              :scope-alias "p"
                                                              :description "The top project."
                                                              :includes ["readme", "dev-guide"]
                                                              :projects [{:name "Project1"
                                                                          :scope "proj1"
                                                                          :scope-alias "p1"
                                                                          :description "The project1."
                                                                          :includes ["readme-p1", "dev-guide-p1"]}
                                                                         {:name "Project2"
                                                                          :scope "proj2"
                                                                          :scope-alias "p2"
                                                                          :description "The project2."
                                                                          :includes ["readme-p2", "dev-guide-p2"]
                                                                          :projects [{:name "Project1-1"
                                                                                      :scope "proj1-1"
                                                                                      :scope-alias "p1-1"
                                                                                      :description "The project1-1."
                                                                                      :includes ["readme-p1-1", "dev-guide-p1-1"]}]}]
                                                              :artifacts [{:name "Artifact1"
                                                                           :scope "art1"
                                                                           :scope-alias "a1"
                                                                           :description "The artifact1."
                                                                           :includes ["readme-a1", "dev-guide-a1"]}
                                                                          {:name "Artifact2"
                                                                           :scope "art2"
                                                                           :scope-alias "a2"
                                                                           :description "The artifact2."
                                                                           :includes ["readme-a2", "dev-guide-a2"]}]}} [:project :projects 1])]
              (is (= ["\\e[1m\\e[31mPROJECT----------\\033[0m\\e[0m" "\\e[1m\\e[31m  Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mname-path : Top Project\\033[0m\\e[0m" "    \\e[1m\\e[31mdescr     : The top project.\\033[0m\\e[0m" "    \\e[1m\\e[31mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[1m\\e[31mscope-path: proj\\033[0m\\e[0m" "    \\e[1m\\e[31malias-path: p\\033[0m\\e[0m" "\\e[1m\\e[31m      Project2\\033[0m\\e[0m" "        \\e[1m\\e[31mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[1m\\e[31mdescr     : The project2.\\033[0m\\e[0m" "        \\e[1m\\e[31mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[1m\\e[31mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[1m\\e[31malias-path: p.p2\\033[0m\\e[0m"] (:output v)))
              (is (= [{:parent-name-path ["Top Project" "Project2"], :parent-scope-path ["proj" "proj2"], :parent-alias-path ["p" "p2"], :level 3, :type :projects, :path [:project :projects 1 :projects 0]}] (:stack v))))))


;;todo
(deftest compute-display-config-test
  (let [config-root {:project {:name "Top Project"
                               :scope "proj"
                               :scope-alias "p"
                               :description "The top project."
                               :includes ["readme", "dev-guide"]}}
        config-artifacts-only {:project {:name "Top Project"
                                         :scope "proj"
                                         :scope-alias "p"
                                         :description "The top project."
                                         :includes ["readme", "dev-guide"]
                                         :artifacts [{:name "Artifact1"
                                                      :scope "art1"
                                                      :scope-alias "a1"
                                                      :description "The artifact1."
                                                      :includes ["readme-a1", "dev-guide-a1"]}
                                                     {:name "Artifact2"
                                                      :scope "art2"
                                                      :scope-alias "a2"
                                                      :description "The artifact2."
                                                      :includes ["readme-a2", "dev-guide-a2"]}]}}
        config-projects-only {:project {:name "Top Project"
                                        :scope "proj"
                                        :scope-alias "p"
                                        :description "The top project."
                                        :includes ["readme", "dev-guide"]
                                        :projects [{:name "Project1"
                                                    :scope "proj1"
                                                    :scope-alias "p1"
                                                    :description "The project1."
                                                    :includes ["readme-p1", "dev-guide-p1"]}
                                                   {:name "Project2"
                                                    :scope "proj2"
                                                    :scope-alias "p2"
                                                    :description "The project2."
                                                    :includes ["readme-p2", "dev-guide-p2"]
                                                    :projects [{:name "Project1-1"
                                                                :scope "proj1-1"
                                                                :scope-alias "p1-1"
                                                                :description "The project1-1."
                                                                :includes ["readme-p1-1", "dev-guide-p1-1"]}]}]}}
        config-projects-and-artifacts {:project {:name "Top Project"
                                                 :scope "proj"
                                                 :scope-alias "p"
                                                 :description "The top project."
                                                 :includes ["readme", "dev-guide"]
                                                 :projects [{:name "Project1"
                                                             :scope "proj1"
                                                             :scope-alias "p1"
                                                             :description "The project1."
                                                             :includes ["readme-p1", "dev-guide-p1"]}
                                                            {:name "Project2"
                                                             :scope "proj2"
                                                             :scope-alias "p2"
                                                             :description "The project2."
                                                             :includes ["readme-p2", "dev-guide-p2"]
                                                             :projects [{:name "Project1-1"
                                                                         :scope "proj1-1"
                                                                         :scope-alias "p1-1"
                                                                         :description "The project1-1."
                                                                         :includes ["readme-p1-1", "dev-guide-p1-1"]}]}]
                                                 :artifacts [{:name "Artifact1"
                                                              :scope "art1"
                                                              :scope-alias "a1"
                                                              :description "The artifact1."
                                                              :includes ["readme-a1", "dev-guide-a1"]}
                                                             {:name "Artifact2"
                                                              :scope "art2"
                                                              :scope-alias "a2"
                                                              :description "The artifact2."
                                                              :includes ["readme-a2", "dev-guide-a2"]}]}}]
  
  ;; no json path
  (testing "root project, no json path"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config config-root {}))))
  (testing "artifacts only, no json path"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a1, dev-guide-a1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a1\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a2, dev-guide-a2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a2\\033[0m\\e[0m"] (d/compute-display-config config-artifacts-only {}))))
  (testing "projects only, no json path"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p1\\033[0m\\e[0m" "\\e[0m\\e[1m      Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p2\\033[0m\\e[0m" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m          Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mname-path : Top Project.Project2.Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mdescr     : The project1-1.\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme-p1-1, dev-guide-p1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.proj2.proj1-1\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.p2.p1-1\\033[0m\\e[0m"] (d/compute-display-config config-projects-only {}))))
  (testing "projects and artifacts, no json path"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a1, dev-guide-a1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a1\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a2, dev-guide-a2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a2\\033[0m\\e[0m" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p1\\033[0m\\e[0m" "\\e[0m\\e[1m      Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p2\\033[0m\\e[0m" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m          Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mname-path : Top Project.Project2.Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mdescr     : The project1-1.\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme-p1-1, dev-guide-p1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.proj2.proj1-1\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.p2.p1-1\\033[0m\\e[0m"] (d/compute-display-config config-projects-and-artifacts {}))))
  
  ;; json path - root
  (testing "root project, json path - root"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m"] (d/compute-display-config config-root {}))))
  (testing "artifacts only, json path - root"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a1, dev-guide-a1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a1\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a2, dev-guide-a2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a2\\033[0m\\e[0m"] (d/compute-display-config config-artifacts-only {}))))
  (testing "projects only, json path - root"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p1\\033[0m\\e[0m" "\\e[0m\\e[1m      Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p2\\033[0m\\e[0m" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m          Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mname-path : Top Project.Project2.Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mdescr     : The project1-1.\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme-p1-1, dev-guide-p1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.proj2.proj1-1\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.p2.p1-1\\033[0m\\e[0m"] (d/compute-display-config config-projects-only {}))))
  (testing "projects and artifacts, json path - root"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a1, dev-guide-a1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a1\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a2, dev-guide-a2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a2\\033[0m\\e[0m" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p1\\033[0m\\e[0m" "\\e[0m\\e[1m      Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p2\\033[0m\\e[0m" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m          Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mname-path : Top Project.Project2.Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mdescr     : The project1-1.\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme-p1-1, dev-guide-p1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.proj2.proj1-1\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.p2.p1-1\\033[0m\\e[0m"] (d/compute-display-config config-projects-and-artifacts {}))))
  
  ;; json path - project w/o children
  (testing "projects and artifacts, json path - projects without children"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a1, dev-guide-a1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a1\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a2, dev-guide-a2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a2\\033[0m\\e[0m" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p1\\033[0m\\e[0m" "\\e[0m\\e[1m      Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p2\\033[0m\\e[0m" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m          Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mname-path : Top Project.Project2.Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mdescr     : The project1-1.\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme-p1-1, dev-guide-p1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.proj2.proj1-1\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.p2.p1-1\\033[0m\\e[0m"] (d/compute-display-config config-projects-and-artifacts {}))))
  
  ;; json path - project w/ children
  (testing "projects and artifacts, json path - projects with children"
    (is (= ["\\e[0m\\e[1mPROJECT----------\\033[0m\\e[0m" "\\e[0m\\e[1m  Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mname-path : Top Project\\033[0m\\e[0m" "    \\e[0m\\e[1mdescr     : The top project.\\033[0m\\e[0m" "    \\e[0m\\e[1mincludes  : readme, dev-guide\\033[0m\\e[0m" "    \\e[0m\\e[1mscope-path: proj\\033[0m\\e[0m" "    \\e[0m\\e[1malias-path: p\\033[0m\\e[0m" "\\e[0m\\e[1m    ARTIFACTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a1, dev-guide-a1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a1\\033[0m\\e[0m" "\\e[0m\\e[1m      Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Artifact2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The artifact2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-a2, dev-guide-a2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.art2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.a2\\033[0m\\e[0m" "\\e[0m\\e[1m    PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m      Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project1\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project1.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p1, dev-guide-p1\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj1\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p1\\033[0m\\e[0m" "\\e[0m\\e[1m      Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mname-path : Top Project.Project2\\033[0m\\e[0m" "        \\e[0m\\e[1mdescr     : The project2.\\033[0m\\e[0m" "        \\e[0m\\e[1mincludes  : readme-p2, dev-guide-p2\\033[0m\\e[0m" "        \\e[0m\\e[1mscope-path: proj.proj2\\033[0m\\e[0m" "        \\e[0m\\e[1malias-path: p.p2\\033[0m\\e[0m" "\\e[0m\\e[1m        PROJECTS---------\\033[0m\\e[0m" "\\e[0m\\e[1m          Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mname-path : Top Project.Project2.Project1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mdescr     : The project1-1.\\033[0m\\e[0m" "            \\e[0m\\e[1mincludes  : readme-p1-1, dev-guide-p1-1\\033[0m\\e[0m" "            \\e[0m\\e[1mscope-path: proj.proj2.proj1-1\\033[0m\\e[0m" "            \\e[0m\\e[1malias-path: p.p2.p1-1\\033[0m\\e[0m"] (d/compute-display-config config-projects-and-artifacts {}))))))


;;todo
(deftest perform-main-test
  (let [config-root {:project {:name "Top Project"
                               :scope "proj"
                               :scope-alias "p"
                               :description "The top project."
                               :includes ["readme", "dev-guide"]}}
        config-artifacts-only {:project {:name "Top Project"
                                         :scope "proj"
                                         :scope-alias "p"
                                         :description "The top project."
                                         :includes ["readme", "dev-guide"]
                                         :artifacts [{:name "Artifact1"
                                                      :scope "art1"
                                                      :scope-alias "a1"
                                                      :description "The artifact1."
                                                      :includes ["readme-a1", "dev-guide-a1"]}
                                                     {:name "Artifact2"
                                                      :scope "art2"
                                                      :scope-alias "a2"
                                                      :description "The artifact2."
                                                      :includes ["readme-a2", "dev-guide-a2"]}]}}
        config-projects-only {:project {:name "Top Project"
                                        :scope "proj"
                                        :scope-alias "p"
                                        :description "The top project."
                                        :includes ["readme", "dev-guide"]
                                        :projects [{:name "Project1"
                                                    :scope "proj1"
                                                    :scope-alias "p1"
                                                    :description "The project1."
                                                    :includes ["readme-p1", "dev-guide-p1"]}
                                                   {:name "Project2"
                                                    :scope "proj2"
                                                    :scope-alias "p2"
                                                    :description "The project2."
                                                    :includes ["readme-p2", "dev-guide-p2"]
                                                    :projects [{:name "Project1-1"
                                                                :scope "proj1-1"
                                                                :scope-alias "p1-1"
                                                                :description "The project1-1."
                                                                :includes ["readme-p1-1", "dev-guide-p1-1"]}]}]}}
        config-projects-and-artifacts {:project {:name "Top Project"
                                                 :scope "proj"
                                                 :scope-alias "p"
                                                 :description "The top project."
                                                 :includes ["readme", "dev-guide"]
                                                 :projects [{:name "Project1"
                                                             :scope "proj1"
                                                             :scope-alias "p1"
                                                             :description "The project1."
                                                             :includes ["readme-p1", "dev-guide-p1"]}
                                                            {:name "Project2"
                                                             :scope "proj2"
                                                             :scope-alias "p2"
                                                             :description "The project2."
                                                             :includes ["readme-p2", "dev-guide-p2"]
                                                             :projects [{:name "Project1-1"
                                                                         :scope "proj1-1"
                                                                         :scope-alias "p1-1"
                                                                         :description "The project1-1."
                                                                         :includes ["readme-p1-1", "dev-guide-p1-1"]}]}]
                                                 :artifacts [{:name "Artifact1"
                                                              :scope "art1"
                                                              :scope-alias "a1"
                                                              :description "The artifact1."
                                                              :includes ["readme-a1", "dev-guide-a1"]}
                                                             {:name "Artifact2"
                                                              :scope "art2"
                                                              :scope-alias "a2"
                                                              :description "The artifact2."
                                                              :includes ["readme-a2", "dev-guide-a2"]}]}}]
  ;; error: options
  (testing "error: ")
  
  ;; error: config file

     ;; error reading config file

     ;; error validating config file
  
  ;; error: finding scope alias path

  ;; success
     ;; must always use specified config file...
     ;; no json path
     ;; json path - root
     ;; json path - project w/o children
     ;; json path - project w/ children
  
  ))
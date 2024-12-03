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
;;	   Project site: https://github.com/kineticfire-labs/semver-multi


(ns semver-multi.common.file-test
  (:require [clojure.test             :refer [deftest is testing]]
            [clojure.string           :as str]
            [babashka.classpath       :as cp]
            [clojure.java.io          :as io]
            [semver-multi.common.file :as cfile])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/file")

(def ^:const resources-test-dir-string "test/resources/file")


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


(defn create-temp-sub-dir
  "Creates a a temp sub-directory 'sub-dir' under the temp directory 'temp-dir-string'."
  [sub-dir]
  (let [temp-dir (File. (str temp-dir-string "/" sub-dir))]
    (.mkdirs temp-dir)))


(defn setup-temp-dir
  "Sets up the temporary directory for the tests in this file.  Creates the directory if it does not exists, recursively deleting the directory first if it does exist."
  []
  (let [ temp-dir (File. temp-dir-string)]
    (when (.exists temp-dir)
      (delete-dir temp-dir))
    (.mkdirs temp-dir)))


;; Configures temporary directory for tests
(setup-temp-dir)




(deftest read-file-test
  (let [local-resources-test-dir-string-slash (str resources-test-dir-string "/read-file/")]
    (testing "file not found"
      (let [v (cfile/read-file (str local-resources-test-dir-string-slash "does-not-exist.txt"))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "File '" local-resources-test-dir-string-slash "does-not-exist.txt' not found"))))))
    (testing "file ok"
      (let [v (cfile/read-file (str local-resources-test-dir-string-slash "file-to-read.txt"))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (string? (:result v)))
        (is (= "This is a\n\nmulti-line file to read\n" (:result v)))))))


(deftest write-file-test
  (let [local-test-sub-dir "write-file"
        local-test-dir-string-slash (str temp-dir-string "/" local-test-sub-dir "/")]
    (create-temp-sub-dir local-test-sub-dir)
    (testing "file not found"
      (let [v (cfile/write-file (str local-test-dir-string-slash "does-not-exist/file.txt") "Line 1\nLine 2\nLine 3")]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "File '" local-test-dir-string-slash "does-not-exist/file.txt' not found"))))))
    (testing "file ok"
      (let [content "Line 1\nLine 2\nLine 3"
            out-file-string (str local-test-dir-string-slash "write-file-ok.txt")
            v (cfile/write-file out-file-string content)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (= content (slurp out-file-string)))))))


(deftest parse-json-file-test
  (let [local-resources-test-dir-string-slash (str resources-test-dir-string "/parse-json-file/")]
    (testing "file not found"
      (let [v (cfile/parse-json-file (str local-resources-test-dir-string-slash "does-not-exist.json"))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "File '" local-resources-test-dir-string-slash "does-not-exist.json' not found"))))))
    (testing "parse fail"
      (let [v (cfile/parse-json-file (str local-resources-test-dir-string-slash "parse-bad.json"))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "JSON parse error when reading file '" local-resources-test-dir-string-slash "parse-bad.json'."))))))
    (testing "parse ok"
      (let [v (cfile/parse-json-file (str local-resources-test-dir-string-slash "parse-good.json"))]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (map? (:result v)))
        (is (= "hi" (:cb (:c (:result v)))))))))


(deftest get-input-file-data-test
  ;; project def file
  (let [local-resources-test-dir-string-slash (str resources-test-dir-string "/get-input-file-data/")]
    (testing "fail: project def file not found"
      (let [v (cfile/get-input-file-data {:project-def-file (str local-resources-test-dir-string-slash "does-not-exist.json")})]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "File '" local-resources-test-dir-string-slash "does-not-exist.json' not found"))))))
    (testing "fail: project def file has parse error"
      (let [v (cfile/get-input-file-data {:project-def-file (str local-resources-test-dir-string-slash "project-def-parse-fail.json")})]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) (str "JSON parse error when reading file '" local-resources-test-dir-string-slash "project-def-parse-fail.json'"))))))
    (testing "success: project def file"
      (let [v (cfile/get-input-file-data {:project-def-file (str local-resources-test-dir-string-slash "project-def-good.json")})]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (map? (:project-def-json v)))
        (is (= "hi" (:cb (:c (:project-def-json v)))))))
         ;; version file
    (testing "fail: version file has 'markers not found' error"
      (let [v (cfile/get-input-file-data {:version-file (str local-resources-test-dir-string-slash "version-markers-fail.dat")})]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) "Could not find start/end markers")))))
    (testing "fail: version file has parse error"
      (let [v (cfile/get-input-file-data {:version-file (str local-resources-test-dir-string-slash "version-parse-fail.dat")})]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (string? (:reason v)))
        (is (true? (str/includes? (:reason v) "JSON parse error when parsing input data")))))
    (testing "success: version file"
      (let [v (cfile/get-input-file-data {:version-file (str local-resources-test-dir-string-slash "version-good.dat")})]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (is (= "hi" (:fb (:f (:version-json v)))))))))
;; todo: test combinations of files above


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


(ns util.semver-ver.core-test
  (:require [clojure.test                      :refer [deftest is testing]]
            [babashka.classpath                :as cp]
            [babashka.process                  :refer [shell]]
            [clojure.java.io                   :as io]
            [util.semver-ver.core              :as ver]
            [common.core                       :as common])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/semver-ver/core-test")

(def ^:const resources-test-data-dir-string "test/resources/semver-ver/data")



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
;; todo disabled for now, until  can update the dynamic test dirs
;;(setup-temp-dir)



(deftest handle-ok-test
  (with-redefs [common/exit-now! (fn [x] x)]
    (testing "exit"
      (is (= 0 (ver/handle-ok))))))


(deftest handle-err-test
  (with-redefs [common/exit-now! (fn [x] x)
                shell (fn [x] (println x))]
    (testing "with message"
      (let [v (with-out-str-data-map (ver/handle-err "The err msg."))]
        (is (= 1 (:result v)))
        (is (= "The err msg.\n" (:str v)))))))


(deftest flag?-test
  (testing "is flag, one dash"
    (let [v (ver/flag? "-flag")]
      (is (boolean? v))
      (is (true? v))))
  (testing "is flag, two dashes"
    (let [v (ver/flag? "--flag")]
      (is (boolean? v))
      (is (true? v))))
  (testing "not flag"
    (let [v (ver/flag? "flag")]
      (is (boolean? v))
      (is (false? v)))))


(deftest mode-defined?-test
  (testing "yes, :create"
    (let [v (ver/mode-defined? [:create])]
      (is (boolean? v))
      (is (true? v))))
  (testing "yes, :validate"
    (let [v (ver/mode-defined? [:validate])]
      (is (boolean? v))
      (is (true? v))))
  (testing "yes, :tag"
    (let [v (ver/mode-defined? [:tag])]
      (is (boolean? v))
      (is (true? v))))
  (testing "no, empty"
    (let [v (ver/mode-defined? [])]
      (is (boolean? v))
      (is (false? v))))
  (testing "no, not empty"
    (let [v (ver/mode-defined? [:test])]
      (is (boolean? v))
      (is (false? v)))))


(defn perform-test-handle-mode-fail
 [defined mode]
  (let [v (ver/handle-mode {} defined [] mode)]
    (is (false? (:success v)))
    (is (= (:reason v) "Duplicate mode defined."))))


(defn perform-test-handle-mode-success
  [mode]
  (let [v (ver/handle-mode {:success true} [:test] ["next"] mode)
        response (:response v)
        defined (:defined v)
        args (:args v)]
    (is (true? (:success v)))
    (is (true? (:success response)))
    (is (= (:mode response) mode))
    (is (= (count defined) 2))
    (is (= (nth defined 0) :test))
    (is (= (nth defined 1) mode))
    (is (= (count args) 0))))


(deftest handle-mode-test
  (testing "success"
    (perform-test-handle-mode-success :create)
    (perform-test-handle-mode-success :validate)
    (perform-test-handle-mode-success :fail))
  (testing "fail"
    (perform-test-handle-mode-fail [:create] :create)
    (perform-test-handle-mode-fail [:validate] :create)
    (perform-test-handle-mode-fail [:tag] :create)
    (perform-test-handle-mode-fail [:create] :validate)
    (perform-test-handle-mode-fail [:validate] :validate)
    (perform-test-handle-mode-fail [:tag] :validate)
    (perform-test-handle-mode-fail [:create] :tag)
    (perform-test-handle-mode-fail [:validate] :tag)
    (perform-test-handle-mode-fail [:tag] :tag)))


(defn perform-test-process-options-mode-fail
  [result]
  (is (false? (:success result)))
  (is (= (:reason result) "Duplicate mode defined.")))


(defn perform-test-process-options-mode-success
  [result expected-mode]
  (let [response (:response result)
        defined (:defined result)
        args (:args result)]
    (is (true? (:success result)))
    (is (true? (:success response)))
    (is (= (:mode response) expected-mode))
    (is (= (count defined) 2))
    (is (= (nth defined 0) :test))
    (is (= (nth defined 1) expected-mode))
    (is (= (count args) 0))))


(deftest process-options-mode-test
  (testing "success"
    (perform-test-process-options-mode-success (ver/process-options-mode-create {:success true} [:test] ["next"]) :create)
    (perform-test-process-options-mode-success (ver/process-options-mode-validate {:success true} [:test] ["next"]) :validate)
    (perform-test-process-options-mode-success (ver/process-options-mode-tag {:success true} [:test] ["next"]) :tag))
  (testing "fail"
    (perform-test-process-options-mode-fail (ver/process-options-mode-create {} [:create] :create))
    (perform-test-process-options-mode-fail (ver/process-options-mode-create {} [:validate] :create))
    (perform-test-process-options-mode-fail (ver/process-options-mode-create {} [:tag] :create))
    (perform-test-process-options-mode-fail (ver/process-options-mode-validate {} [:create] :validate))
    (perform-test-process-options-mode-fail (ver/process-options-mode-validate {} [:validate] :validate))
    (perform-test-process-options-mode-fail (ver/process-options-mode-validate {} [:tag] :validate))
    (perform-test-process-options-mode-fail (ver/process-options-mode-tag {} [:create] :tag))
    (perform-test-process-options-mode-fail (ver/process-options-mode-tag {} [:validate] :tag))
    (perform-test-process-options-mode-fail (ver/process-options-mode-tag {} [:tag] :tag))))


(deftest process-options-no-warn-test
  (testing "fail, duplicate"
    (let [v (ver/process-options-no-warn {} [:no-warn] ["test"])]
      (is (boolean? (:success v)))
      (is (false? (:success v)))))
  (testing "success"
    (let [v (ver/process-options-no-warn {:success true} [:test] ["test"])
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (boolean? (:no-warn response)))
      (is (true? (:no-warn response)))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :no-warn))
      (is (= (count args) 0)))))


(deftest process-options-other-test
  (testing "fail, item is not a flag"
    (let [args ["notflag" "--no-warn"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) "Expected flag but received non-flag 'notflag'."))))
  (testing "fail, no arg after flag"
    (let [args ["--version"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) "Expected argument following flag '--version' but found none."))))
  (testing "fail, flag follows flag"
    (let [args ["--version" "--flag"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) "Expected argument following flag '--version' but found flag '--flag'."))))
  (testing "fail, flag not recognized"
    (let [args ["--flag" "1.2.3"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) "Flag '--flag' not recognized."))))
  (testing "fail, duplicate flag"
    (let [args ["--version" "1.2.3"]
          item (first args)
          v (ver/process-options-other {:success true} [:test :version] args item ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) "Duplicate definition of flag '--version'."))))
  (testing "success, version"
    (let [args ["--version" "1.2.3"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:version response) "1.2.3"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :version))
      (is (= (count args) 0))))
  (testing "success, project-def-file"
    (let [args ["--project-def-file" "/path/to/project-def.json"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:project-def-file response) "/path/to/project-def.json"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :project-def-file))
      (is (= (count args) 0))))
  (testing "success, version-file"
    (let [args ["--version-file" "/path/to/version.file"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:version-file response) "/path/to/version.file"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :version-file))
      (is (= (count args) 0))))
  (testing "success, tag-name"
    (let [args ["--tag-name" "2.3.4"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:tag-name response) "2.3.4"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :tag-name))
      (is (= (count args) 0))))
  (testing "success, non-empty args result"
    (let [args ["--version" "1.2.3" "--project-def-file" "/path/to/project-def.json"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:version response) "1.2.3"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :version))
      (is (= (count args) 2))
      (is (= (nth args 0) "--project-def-file"))
      (is (= (nth args 1) "/path/to/project-def.json")))))


(deftest check-response-keys-test
  (testing "fail, has unrecognized keys"
    (let [v (ver/check-response-keys {:success true :a 1 :b 2 :z 26} :testing [:a :b] [:c])]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :testing doesn't allow keys ':z'."))))
  (testing "fail, missing required keys"
    (let [v (ver/check-response-keys {:success true :a 1 :c 3} :testing [:a :b] [:c])]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :testing requires missing keys ':b'."))))
  (testing "success, required keys, no optional keys allowed"
    (let [v (ver/check-response-keys {:success true :a 1 :b 2} :testing [:a :b] [])]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:a v) 1))
      (is (= (:b v) 2))))
  (testing "success, required keys, no optional keys in response"
    (let [v (ver/check-response-keys {:success true :a 1 :b 2} :testing [:a :b] [:c])]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:a v) 1))
      (is (= (:b v) 2))))
  (testing "success, required keys, with optional keys in response"
    (let [v (ver/check-response-keys {:success true :a 1 :b 2 :c 3} :testing [:a :b] [:c])]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:a v) 1))
      (is (= (:b v) 2))
      (is (= (:c v) 3))))
  (testing "success, no required keys, with optional keys in response"
    (let [v (ver/check-response-keys {:success true :c 3} :testing [] [:c])]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:c v) 3)))))


(deftest check-response-mode-create-test
  (testing "fail, unrecognized key"
    (let [v (ver/check-response-mode-create {:success true :mode :create :no-warn true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create doesn't allow keys ':no-warn'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response-mode-create {:success true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create requires missing keys ':mode'."))))
  (testing "success, no optional keys"
    (let [v (ver/check-response-mode-create {:success true :mode :create})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "success, with all optional keys"
    (let [v (ver/check-response-mode-create {:success true :mode :create :version "1.0.0" :project-def-file "path/to/project-def.json"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "path/to/project-def.json")))))


(deftest check-response-mode-validate-test
  (testing "fail, unrecognized key"
    (let [v (ver/check-response-mode-validate {:success true :mode :validate :version-file "path/to/version.file" :project-def-file "path/to/project-def.json" :no-warn true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate doesn't allow keys ':no-warn'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response-mode-validate {:success true :mode :validate :version-file "path/to/version.file"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate requires missing keys ':project-def-file'."))))
  (testing "success"
    (let [v (ver/check-response-mode-validate {:success true :mode :validate :version-file "path/to/version.file" :project-def-file "path/to/project-def.json"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/version.file"))
      (is (= (:project-def-file v) "path/to/project-def.json")))))


(deftest check-response-mode-tag-test
  (testing "fail, unrecognized key"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :tag-name "1.0.0" :version-file "path/to/version.file" :version "1.0.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag doesn't allow keys ':version'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :tag-name "1.0.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag requires missing keys ':version-file'."))))
  (testing "success, no optional keys"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :tag-name "1.0.0" :version-file "path/to/version.file"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:tag-name v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.file"))))
  (testing "success, with all optional keys"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :tag-name "1.0.0" :version-file "path/to/version.file" :no-warn true})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:tag-name v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.file"))
      (is (boolean? (:no-warn v)))
      (is (true? (:no-warn v))))))


(deftest check-response-test
  ;;
  ;; mode: create
  (testing "create: fail, unrecognized key"
    (let [v (ver/check-response {:success true :mode :create :no-warn true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create doesn't allow keys ':no-warn'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response {:success true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "No mode set. Exactly one mode must be set:  --create, --validate, or --tag."))))
  (testing "success, no optional keys"
    (let [v (ver/check-response {:success true :mode :create})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "create: success, with all optional keys"
    (let [v (ver/check-response {:success true :mode :create :version "1.0.0" :project-def-file "path/to/project-def.json"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "path/to/project-def.json"))))
  ;;
  ;; mode: validate
  (testing "fail, unrecognized key"
    (let [v (ver/check-response {:success true :mode :validate :version-file "path/to/version.file" :project-def-file "path/to/project-def.json" :no-warn true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate doesn't allow keys ':no-warn'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response {:success true :mode :validate :version-file "path/to/version.file"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate requires missing keys ':project-def-file'."))))
  (testing "success"
    (let [v (ver/check-response {:success true :mode :validate :version-file "path/to/version.file" :project-def-file "path/to/project-def.json"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/version.file"))
      (is (= (:project-def-file v) "path/to/project-def.json"))))
  ;;
  ;; mode: tag
  (testing "fail, unrecognized key"
    (let [v (ver/check-response {:success true :mode :tag :tag-name "1.0.0" :version-file "path/to/version.file" :version "1.0.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag doesn't allow keys ':version'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response {:success true :mode :tag :tag-name "1.0.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag requires missing keys ':version-file'."))))
  (testing "success, no optional keys"
    (let [v (ver/check-response {:success true :mode :tag :tag-name "1.0.0" :version-file "path/to/version.file"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:tag-name v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.file"))))
  (testing "success, with all optional keys"
    (let [v (ver/check-response {:success true :mode :tag :tag-name "1.0.0" :version-file "path/to/version.file" :no-warn true})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:tag-name v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.file"))
      (is (boolean? (:no-warn v)))
      (is (true? (:no-warn v))))))


(defn perform-test-process-cli-options-num-args-fail
  [args]
  (let [v (ver/process-cli-options args {})]
    (is (false? (:success v)))
    (is (= (:reason v) (str "Invalid options format. Expected 1, 3, 5, or 6 CLI arguments but received " (count args) " arguments.")))))


(defn perform-test-process-cli-options-duplicate-mode-fail
  [args]
  (let [v (ver/process-cli-options args {})]
    (is (false? (:success v)))
    (is (= (:reason v) (str "Invalid options format. Duplicate mode defined.")))))


(deftest process-cli-options-test
  (testing "fail, num args"
    (perform-test-process-cli-options-num-args-fail [])
    (perform-test-process-cli-options-num-args-fail ["--a" "--b"])
    (perform-test-process-cli-options-num-args-fail ["--a" "--b" "--c" "--d"])
    (perform-test-process-cli-options-num-args-fail ["--a" "--b" "--c" "--d" "--e" "--f" "--g"]))
  (testing "fail, duplicate mode defined"
    (perform-test-process-cli-options-duplicate-mode-fail ["--create" "--create" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--create" "--validate" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--create" "--tag" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--validate" "--create" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--validate" "--validate" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--validate" "--tag" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--tag" "--create" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--tag" "--validate" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--tag" "--tag" "to meet min num args"]))
  (testing "fail, duplicate --no-warn"
    (let [v (ver/process-cli-options ["--no-warn" "--no-warn" "to meet min num args"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Duplicate definition of flag '--no-warn'.")))))
  (testing "fail, expected flag but got non-flag"
    (let [v (ver/process-cli-options ["notflag" "other" "to meet min num args"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Expected flag but received non-flag 'notflag'.")))))
  (testing "fail, expected argument after flag but found none"
    (let [v (ver/process-cli-options ["--version"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Expected argument following flag '--version' but found none.")))))
  (testing "fail, expected argument after flag but found flag"
    (let [v (ver/process-cli-options ["--version" "--unexpected" "to meet min num args"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Expected argument following flag '--version' but found flag '--unexpected'.")))))
  (testing "fail, unrecognized flag"
    (let [v (ver/process-cli-options ["--unknown" "5" "to meet min num args"] ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Flag '--unknown' not recognized.")))))
  (testing "fail, duplicate definition of flag"
    (let [v (ver/process-cli-options ["--version" "5" "--version" "5" "to meet min num args"] ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Duplicate definition of flag '--version'.")))))
  
  ;;
  ;; w/ check-response
  ;;

  ;;
  ;; mode: create
  (testing "create: fail, unrecognized key"
    (let [v (ver/process-cli-options ["--create" "--version-file" "path/to/version.file"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create doesn't allow keys ':version-file'."))))
  (testing "create: success, no optional keys"
    (let [v (ver/process-cli-options ["--create"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :create))))
  (testing "create: success, with all optional keys"
    (let [v (ver/process-cli-options ["--create" "--version" "1.0.0" "--project-def-file" "path/to/project-def.json"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :create))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "path/to/project-def.json"))))
  ;;
  ;; mode: validate
  (testing "fail, unrecognized key"
    (let [v (ver/process-cli-options ["--validate" "--version-file" "path/to/version.file" "--project-def-file" "path/to/project-def.json" "--no-warn"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate doesn't allow keys ':no-warn'."))))
  (testing "fail, missing required key"
    (let [v (ver/process-cli-options ["--validate" "--version-file" "path/to/version.file"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate requires missing keys ':project-def-file'."))))
  (testing "success"
    (let [v (ver/process-cli-options ["--validate" "--version-file" "path/to/version.file" "--project-def-file" "path/to/project-def.json"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :validate))
      (is (= (:version-file v) "path/to/version.file"))
      (is (= (:project-def-file v) "path/to/project-def.json"))))
  ;;
  ;; mode: tag
  (testing "fail, unrecognized key"
    (let [v (ver/process-cli-options ["--tag" "--tag-name" "1.0.0" "--project-def-file" "path/to/project-def.json"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag doesn't allow keys ':project-def-file'."))))
  (testing "fail, missing required key"
    (let [v (ver/process-cli-options ["--tag" "--tag-name" "1.0.0"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag requires missing keys ':version-file'."))))
  (testing "success, no optional keys"
    (let [v (ver/process-cli-options ["--tag" "--tag-name" "1.0.0" "--version-file" "path/to/version.file"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :tag))
      (is (= (:tag-name v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.file"))))
  (testing "success, with all optional keys"
    (let [v (ver/process-cli-options ["--tag" "--tag-name" "1.0.0" "--version-file" "path/to/version.file" "--no-warn"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :tag))
      (is (= (:tag-name v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.file"))
      (is (boolean? (:no-warn v)))
      (is (true? (:no-warn v))))))


(deftest apply-default-options-mode-create-test
  (testing "all options set"
    (let [v (ver/apply-default-options-mode-create {:version "2.3.4" :project-def-file "other/path/to/myproject-def.json"} "the/path/to" "project-def.json")]
      (is (= (count v) 2))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "other/path/to/myproject-def.json"))))
  (testing "no options set, assuming not in git"
    (let [v (ver/apply-default-options-mode-create {} "the/path/to" "project-def.json")]
      (is (= (count v) 2))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "the/path/to/project-def.json")))))


(deftest apply-default-options-mode-validate-test
  (testing "returns options unchanged"
    (let [v (ver/apply-default-options-mode-validate {:a 1 :b 2})]
      (is (= (count v) 2))
      (is (= (:a v) 1))
      (is (= (:b v) 2)))))


(deftest apply-default-options-mode-tag-test
  (testing "returns options unchanged"
    (let [v (ver/apply-default-options-mode-tag {:a 1 :b 2})]
      (is (= (count v) 2))
      (is (= (:a v) 1))
      (is (= (:b v) 2)))))


(deftest apply-default-options-test
  (testing "create: all options set"
    (let [v (ver/apply-default-options {:mode :create :version "2.3.4" :project-def-file "other/path/to/myproject-def.json"} "the/path/to" "project-def.json")]
      (is (= (count v) 3))
      (is (= (:mode v) :create))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "other/path/to/myproject-def.json"))))
  (testing "create: no options set"
    (let [v (ver/apply-default-options {:mode :create } "the/path/to" "project-def.json")]
      (is (= (count v) 3))
      (is (= (:mode v) :create))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "the/path/to/project-def.json"))))
  (testing "validate: returns options unchanged"
    (let [v (ver/apply-default-options {:mode :validate :a 1 :b 2} "the/path/to" "project-def.json")]
      (is (= (count v) 3))
      (is (= (:mode v) :validate))
      (is (= (:a v) 1))
      (is (= (:b v) 2))))
  (testing "tag: returns options unchanged"
    (let [v (ver/apply-default-options {:mode :tag :a 1 :b 2} "the/path/to" "project-def.json")]
      (is (= (count v) 3))
      (is (= (:mode v) :tag))
      (is (= (:a v) 1))
      (is (= (:b v) 2)))))

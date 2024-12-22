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


;; KineticFire Labs: https://labs.kineticfire.com/
;;	   Project site: https://github.com/kineticfire-labs/semver-multi/


(ns semver-multi.util.semver-ver.core-test
  (:require [clojure.test                      :refer [deftest is testing]]
            [babashka.classpath                :as cp]
            [babashka.process                  :refer [shell]]
            [clojure.string                    :as str]
            [clojure.java.io                   :as io]
            [semver-multi.common.system        :as system]
            [semver-multi.common.version       :as version]
            [semver-multi.util.semver-ver.core :as ver])
  (:import (java.io File)))


(cp/add-classpath "./")


(def ^:const temp-dir-string "gen/test/semver-ver")

(def ^:const resources-test-dir-string "test/resources/semver-ver")



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
  (let [temp-dir (File. temp-dir-string)]
    (when (.exists temp-dir)
      (delete-dir temp-dir))
    (.mkdirs temp-dir)))


;; Configures temporary directory for tests
(setup-temp-dir)



(deftest handle-ok-test
  (with-redefs [system/exit-now! (fn [x] x)]
    (testing "exit"
      (is (= 0 (ver/handle-ok))))))


(deftest handle-err-test
  (with-redefs [system/exit-now! (fn [x] x)
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


(deftest process-options-other-test
  (testing "fail, item is not a flag"
    (let [args ["notflag" "--version 1.0.0"]
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
  (testing "success, version-file"
    (let [args ["--version-file" "/path/to/version.dat"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)
          args (:args v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:version-file response) "/path/to/version.dat"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :version-file))
      (is (= (count args) 0))))
  (testing "success, non-empty args result"
    (let [args ["--version" "1.2.3"]
          item (first args)
          v (ver/process-options-other {:success true} [:test] args item ver/cli-flags-non-mode)
          response (:response v)
          defined (:defined v)]
      (is (true? (:success v)))
      (is (true? (:success response)))
      (is (= (:version response) "1.2.3"))
      (is (= (count defined) 2))
      (is (= (nth defined 0) :test))
      (is (= (nth defined 1) :version)))))


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


(defn perform-is-create-type?-test
  [type result]
  (let [v (ver/is-create-type? type)]
    (is (boolean? v))
    (is (= v result))))


(deftest is-create-type?
  (testing "ok - release"
    (perform-is-create-type?-test "release" true))
  (testing "ok - update"
    (perform-is-create-type?-test "update" true))
  (testing "fail - not known"
    (perform-is-create-type?-test "other" false))
  (testing "fail - empty string"
    (perform-is-create-type?-test "" false))
  (testing "fail - nil"
    (perform-is-create-type?-test nil false)))


(defn perform-is-optional-create-type?-test
  [type result]
  (let [v (ver/is-optional-create-type? type)]
    (is (boolean? v))
    (is (= v result))))


(deftest is-optional-create-type?
  (testing "ok - release"
    (perform-is-optional-create-type?-test "release" true))
  (testing "ok - update"
    (perform-is-optional-create-type?-test "update" true))
  (testing "fail - not known"
    (perform-is-optional-create-type?-test "other" false))
  (testing "fail - empty string"
    (perform-is-optional-create-type?-test "" false))
  (testing "ok - nil"
    (perform-is-optional-create-type?-test nil true)))


(defn perform-is-optional-semantic-version-release?-test
  [version result]
  (let [v (ver/is-optional-semantic-version-release? version)]
    (is (boolean? v))
    (is (= v result))))


(deftest is-optional-semantic-version-release?
  (testing "ok - release"
    (perform-is-optional-semantic-version-release?-test "1.0.0" true))
  (testing "ok - not provided"
    (perform-is-optional-semantic-version-release?-test nil true))
  (testing "fail - not a release"
    (perform-is-optional-semantic-version-release?-test "1.0.0-alpha.beta" false))
  (testing "fail - empty string"
    (perform-is-optional-semantic-version-release?-test "" false))
  (testing "fail - invalid"
    (perform-is-optional-semantic-version-release?-test "1.abc.0" false)))


(deftest check-response-mode-create-test
  (testing "fail, unrecognized key"
    (let [v (ver/check-response-mode-create {:success true :mode :create :remote-name "other"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create doesn't allow keys ':remote-name'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response-mode-create {:success true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create requires missing keys ':mode'."))))
  (testing "fail, bad type"
    (let [v (ver/check-response-mode-create {:success true :mode :create :type "invalid"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Argument ':type' must be either 'release', 'test-release', or 'update' but was 'invalid'."))))
  (testing "fail, bad version"
    (let [v (ver/check-response-mode-create {:success true :mode :create :version "1.abc.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Argument ':version' must be a valid semantic version release number but was '1.abc.0'."))))
  (testing "success, no optional keys"
    (let [v (ver/check-response-mode-create {:success true :mode :create})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "success, with all optional keys"
    (let [v (ver/check-response-mode-create {:success true :mode :create :type "release" :version "1.0.0" :version-file "path/to/myversion.dat"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:type v) "release"))
      (is (= (:version v) "1.0.0"))
      (is (= (:version-file v) "path/to/myversion.dat")))))


(deftest check-response-mode-validate-test
  (testing "fail, unrecognized key"
    (let [v (ver/check-response-mode-validate {:success true :mode :validate :version-file "path/to/myversion.dat" :remote-name "other"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate doesn't allow keys ':remote-name'."))))
  (testing "success, no optional parameters"
    (let [v (ver/check-response-mode-validate {:success true :mode :validate})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "success, all optional parameters"
    (let [v (ver/check-response-mode-validate {:success true :mode :validate :version-file "path/to/myversion.dat"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/myversion.dat")))))


(deftest check-response-mode-tag-test
  (testing "fail, unrecognized key"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :version-file "path/to/version.dat" :version "1.0.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag doesn't allow keys ':version'."))))
  (testing "fail, missing required key"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag requires missing keys ':version-file'."))))
  (testing "success, no optional keys"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :version-file "path/to/version.dat"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/version.dat"))))
  (testing "success, with all optional keys"
    (let [v (ver/check-response-mode-tag {:success true :mode :tag :version-file "path/to/version.dat" :remote-name "other"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/version.dat"))
      (is (= (:remote-name v) "other")))))


(deftest check-response-test
  ;;
  ;; general
  (testing "general: fail, missing required key"
    (let [v (ver/check-response {:success true})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "No mode set. Exactly one mode must be set:  --create, --validate, or --tag."))))
  ;;
  ;; mode: create
  (testing "create: fail, unrecognized key"
    (let [v (ver/check-response {:success true :mode :create :remote-name "other"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create doesn't allow keys ':remote-name'."))))
  (testing "create: fail, invalid type"
    (let [v (ver/check-response {:success true :mode :create :type "invalid"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Argument ':type' must be either 'release', 'test-release', or 'update' but was 'invalid'."))))
  (testing "create: success, no optional keys"
    (let [v (ver/check-response {:success true :mode :create})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "create: success, with all optional keys"
    (let [v (ver/check-response {:success true :mode :create :type "release" :version "1.0.0" :version-file "path/to/version.dat"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:type v) "release"))
      (is (= (:version v) "1.0.0"))
      (is (= (:version-file v) "path/to/version.dat"))))
  ;;
  ;; mode: validate
  (testing "validate: fail, unrecognized key"
    (let [v (ver/check-response {:success true :mode :validate :version-file "path/to/myversion.dat" :remote-name "other"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate doesn't allow keys ':remote-name'."))))
  (testing "validate: success, no optional parameters"
    (let [v (ver/check-response {:success true :mode :validate})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))))
  (testing "validate: success, all optional parameters"
    (let [v (ver/check-response {:success true :mode :validate :version-file "path/to/myversion.dat"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  ;;
  ;; mode: tag
  (testing "tag: fail, unrecognized key"
    (let [v (ver/check-response {:success true :mode :tag :version-file "path/to/version.dat" :version "1.0.0"})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag doesn't allow keys ':version'."))))
  (testing "tag: fail, missing required key"
    (let [v (ver/check-response {:success true :mode :tag})]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag requires missing keys ':version-file'."))))
  (testing "tag: success, no optional keys"
    (let [v (ver/check-response {:success true :mode :tag :version-file "path/to/version.dat"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/version.dat"))))
  (testing "tag: success, with all optional keys"
    (let [v (ver/check-response {:success true :mode :tag :version-file "path/to/version.dat" :remote-name "other"})]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:version-file v) "path/to/version.dat"))
      (is (= (:remote-name v) "other")))))


(defn perform-test-process-cli-options-num-args-fail
  [args]
  (let [v (ver/process-cli-options args {})]
    (is (false? (:success v)))
    (is (= (:reason v) (str "Invalid options format. Expected 1 or more arguments but received no arguments.")))))


(defn perform-test-process-cli-options-duplicate-mode-fail
  [args]
  (let [v (ver/process-cli-options args {})]
    (is (false? (:success v)))
    (is (= (:reason v) (str "Invalid options format. Duplicate mode defined.")))))


(deftest process-cli-options-test
  (testing "general: fail, num args"
    (perform-test-process-cli-options-num-args-fail []))
  (testing "general: fail, duplicate mode defined"
    (perform-test-process-cli-options-duplicate-mode-fail ["--create" "--create" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--create" "--validate" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--create" "--tag" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--validate" "--create" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--validate" "--validate" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--validate" "--tag" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--tag" "--create" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--tag" "--validate" "to meet min num args"])
    (perform-test-process-cli-options-duplicate-mode-fail ["--tag" "--tag" "to meet min num args"]))
  (testing "general: fail, expected flag but got non-flag"
    (let [v (ver/process-cli-options ["notflag" "other" "to meet min num args"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Expected flag but received non-flag 'notflag'.")))))
  (testing "general: fail, expected argument after flag but found none"
    (let [v (ver/process-cli-options ["--version"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Expected argument following flag '--version' but found none.")))))
  (testing "general: fail, expected argument after flag but found flag"
    (let [v (ver/process-cli-options ["--version" "--unexpected" "to meet min num args"] {})]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Expected argument following flag '--version' but found flag '--unexpected'.")))))
  (testing "general: fail, unrecognized flag"
    (let [v (ver/process-cli-options ["--unknown" "5" "to meet min num args"] ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Flag '--unknown' not recognized.")))))
  (testing "general: fail, duplicate definition of flag"
    (let [v (ver/process-cli-options ["--version" "5" "--version" "5" "to meet min num args"] ver/cli-flags-non-mode)]
      (is (false? (:success v)))
      (is (= (:reason v) (str "Invalid options format. Duplicate definition of flag '--version'.")))))
  
  ;;
  ;; w/ check-response
  ;;

  ;;
  ;; mode: create
  (testing "create: fail, unrecognized key"
    (let [v (ver/process-cli-options ["--create" "--remote-name" "1.0.0"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :create doesn't allow keys ':remote-name'."))))
  (testing "create: success, no optional keys"
    (let [v (ver/process-cli-options ["--create"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :create))))
  (testing "create: success, with all optional keys"
    (let [v (ver/process-cli-options ["--create" "--version" "1.0.0"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :create))
      (is (= (:version v) "1.0.0"))))
  ;;
  ;; mode: validate
  (testing "validate: fail, unrecognized key"
    (let [v (ver/process-cli-options ["--validate" "--version-file" "path/to/myversion.dat" "--remote-name" "1.0.0"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :validate doesn't allow keys ':remote-name'."))))
  (testing "validate: success, no optional parameters"
    (let [v (ver/process-cli-options ["--validate"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :validate))))
  (testing "validate: success, all optional parameters"
    (let [v (ver/process-cli-options ["--validate" "--version-file" "path/to/myversion.dat"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :validate))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  ;;
  ;; mode: tag
  (testing "tag: fail, unrecognized key"
    (let [v (ver/process-cli-options ["--tag" "--type" "update"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag doesn't allow keys ':type'."))))
  (testing "tag: fail, missing required key"
    (let [v (ver/process-cli-options ["--tag"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (false? (:success v)))
      (is (= (:reason v) "Mode :tag requires missing keys ':version-file'."))))
  (testing "tag: success, no optional keys"
    (let [v (ver/process-cli-options ["--tag" "--version-file" "path/to/version.dat"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :tag))
      (is (= (:version-file v) "path/to/version.dat"))))
  (testing "tag: success, with all optional keys"
    (let [v (ver/process-cli-options ["--tag" "--version-file" "path/to/version.dat" "--remote-name" "remote"] ver/cli-flags-non-mode)]
      (is (boolean? (:success v)))
      (is (true? (:success v)))
      (is (= (:mode v) :tag))
      (is (= (:version-file v) "path/to/version.dat"))
      (is (= (:remote-name v) "remote")))))


(deftest apply-default-options-mode-create-test
  (testing "all options set - with type=update"
    (let [v (ver/apply-default-options-mode-create {:type "update" :version "2.3.4":version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat")]
      (is (= (count v) 4))
      (is (= (:type v) :update))
      (is (= (:version v) "2.3.4"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "all options set - with type=release"
    (let [v (ver/apply-default-options-mode-create {:type "release" :version "2.3.4" :version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat")]
      (is (= (count v) 4))
      (is (= (:type v) :release))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "all options set - with type=test-release"
    (let [v (ver/apply-default-options-mode-create {:type "test-release" :version "2.3.4" :version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat")]
      (is (= (count v) 4))
      (is (= (:type v) :test-release))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "no options set - test default options"
    (let [v (ver/apply-default-options-mode-create {} "the/path/to" "semver-multi.json" "version.dat")]
      (is (= (count v) 4))
      (is (= (:type v) :release))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "version.dat")))))


(deftest apply-default-options-mode-validate-test
  (testing "all options set"
    (let [v (ver/apply-default-options-mode-validate {:version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat")]
      (is (= (count v) 2))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "no options set"
    (let [v (ver/apply-default-options-mode-validate {} "the/path/to" "semver-multi.json" "version.dat")]
      (is (= (count v) 2))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "version.dat")))))


(deftest apply-default-options-mode-tag-test
  (testing "all options set"
    (let [v (ver/apply-default-options-mode-tag {:remote-name "other"} "the/path/to" "semver-multi.json" "origin")]
      (is (= (count v) 2))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:remote-name v) "other"))))
  (testing "no options set"
    (let [v (ver/apply-default-options-mode-tag {} "the/path/to" "semver-multi.json" "origin")]
      (is (= (count v) 2))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:remote-name v) "origin")))))


(deftest apply-default-options-test
  (testing "create: all options set with type=release"
    (let [v (ver/apply-default-options {:mode :create :type "release" :version "2.3.4" :version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 5))
      (is (= (:mode v) :create))
      (is (= (:type v) :release))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "create: all options set with type=test-release"
    (let [v (ver/apply-default-options {:mode :create :type "test-release" :version "2.3.4" :version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 5))
      (is (= (:mode v) :create))
      (is (= (:type v) :test-release))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "create: all options set with type=update"
    (let [v (ver/apply-default-options {:mode :create :type "update" :version "2.3.4" :version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 5))
      (is (= (:mode v) :create))
      (is (= (:type v) :update))
      (is (= (:version v) "2.3.4"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "create: no options set"
    (let [v (ver/apply-default-options {:mode :create } "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 5))
      (is (= (:mode v) :create))
      (is (= (:type v) :release))
      (is (= (:version v) "1.0.0"))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "version.dat"))))
  (testing "validate: all options set"
    (let [v (ver/apply-default-options {:mode :validate :version-file "path/to/myversion.dat"} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 3))
      (is (= (:mode v) :validate))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "path/to/myversion.dat"))))
  (testing "validate: no options set"
    (let [v (ver/apply-default-options {:mode :validate} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 3))
      (is (= (:mode v) :validate))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:version-file v) "version.dat"))))
  (testing "tag: all default options set"
    (let [v (ver/apply-default-options {:mode :tag :remote-name "other"} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 3))
      (is (= (:mode v) :tag))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:remote-name v) "other"))))
  (testing "tag: no options set"
    (let [v (ver/apply-default-options {:mode :tag} "the/path/to" "semver-multi.json" "version.dat" "origin")]
      (is (= (count v) 3))
      (is (= (:mode v) :tag))
      (is (= (:project-def-file v) "the/path/to/semver-multi.json"))
      (is (= (:remote-name v) "origin")))))


(deftest create-release-version-data-test
  (testing "no sub-projects"
    (let [options {:type :release :version "1.0.0"}
          config {:project {:scope "proj"
                            :artifacts [{:scope "proj-art-1"}
                                        {:scope "proj-art-2"}]}}
          v (ver/create-release-version-data options config)]
      (is (= (:type v) "release"))
      (is (= (:project-root v) "proj"))
      (let [versions (:versions v)]
        (is (= (:version (:proj versions)) "1.0.0"))
        (is (= (:version (:proj.proj-art-1 versions)) "1.0.0"))
        (is (= (:version (:proj.proj-art-2 versions)) "1.0.0")))))
  (testing "deep project"
    (let [options {:type :release :version "1.0.0"}
          config {:project {:scope "proj"
                            :artifacts [{:scope "proj-art-1"}
                                        {:scope "proj-art-2"}]
                            :projects [
                                       {:scope "alpha"
                                        :artifacts [{:scope "alpha-art-1"}]
                                        :projects [
                                                   {:scope "charlie"
                                                    :artifacts [{:scope "charlie-art-1"}]}
                                                   {:scope "delta"
                                                    :artifacts [{:scope "delta-art-1"}]}]}
                                       {:scope "bravo"
                                        :artifacts [{:scope "bravo-art-1"}]}]}}
          v (ver/create-release-version-data options config)]
      (is (= (:type v) "release"))
      (is (= (:project-root v) "proj"))
      (let [versions (:versions v)]
        (is (= (:version (:proj versions)) "1.0.0"))
        (is (= (:version (:proj.proj-art-1 versions)) "1.0.0"))
        (is (= (:version (:proj.proj-art-2 versions)) "1.0.0"))
        (is (= (:version (:proj.alpha versions)) "1.0.0"))
        (is (= (:version (:proj.alpha.alpha-art-1 versions)) "1.0.0"))
        (is (= (:version (:proj.alpha.charlie versions)) "1.0.0"))
        (is (= (:version (:proj.alpha.charlie.charlie-art-1 versions)) "1.0.0"))
        (is (= (:version (:proj.alpha.delta versions)) "1.0.0"))
        (is (= (:version (:proj.alpha.delta.delta-art-1 versions)) "1.0.0"))
        (is (= (:version (:proj.bravo versions)) "1.0.0"))
        (is (= (:version (:proj.bravo.bravo-art-1 versions)) "1.0.0"))))))


(deftest perform-mode-create-release-test
  (let [subdir "perform-mode-create-release-test"]
    (create-temp-sub-dir subdir)
    (testing "fail: directory not found"
      (let [output-file (str temp-dir-string "/" subdir "/doesnt-exist/version.dat")
            options {:type :release :version "1.0.0" :version-file output-file}
            config {:project {:scope "proj"
                              :artifacts [{:scope "proj-art-1"}
                                          {:scope "proj-art-2"}]}}
            v (ver/perform-mode-create-release options config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (true? (str/includes? (:reason v) output-file)))))
    (testing "ok"
      (let [output-file (str temp-dir-string "/" subdir "/version.dat")
            options {:type :release :version "1.0.0" :version-file output-file}
            config {:project {:scope "proj"
                              :artifacts [{:scope "proj-art-1"}
                                          {:scope "proj-art-2"}]}}
            v (ver/perform-mode-create-release options config)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (let [result (version/parse-version-data (slurp output-file))]
          (is (true? (:success result)))
          (is (= (:type (:version-json result)) "release"))
          (is (= (:project-root (:version-json result)) "proj"))
          (is (= (:version (:proj.proj-art-1 (:versions (:version-json result)))) "1.0.0")))))))


(deftest perform-mode-create-update-test
  (let [subdir "perform-mode-create-update-test"]
    (create-temp-sub-dir subdir)
    (testing "fail: directory not found"
      (let [output-file (str temp-dir-string "/" subdir "/doesnt-exist/version.dat")
            options {:type :update :version-file output-file}
            v (ver/perform-mode-create-update options)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (false? (:success v)))
        (is (true? (str/includes? (:reason v) output-file)))))
    (testing "ok"
      (let [output-file (str temp-dir-string "/" subdir "/version.dat")
            options {:type :update :version-file output-file}
            v (ver/perform-mode-create-update options)]
        (is (map? v))
        (is (boolean? (:success v)))
        (is (true? (:success v)))
        (let [result (version/parse-version-data (slurp output-file))]
          (is (true? (:success result)))
          (is (= (:type (:version-json result)) "update"))
          (is (= (count (:add (:version-json result))) 1))
          (is (= (:example.alpha (first (:add (:version-json result)))) "1.0.0"))
          (is (= (count (:remove (:version-json result))) 1))
          (is (= (first (:remove (:version-json result))) "example.bravo"))
          (is (= (count (:move (:version-json result))) 1))
          (is (= (:example.from.charlie (:move (:version-json result))) "example.to.echo.charlie")))))))



;; todo: test perform-mode-create


;; todo: test perform-mode


;; todo: validate-version-json-if-present


;; todo: test perform-main
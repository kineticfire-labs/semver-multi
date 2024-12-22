#!/usr/bin/env bb

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


;; KineticFire Labs
;;	  Project site:  https://github.com/kineticfire-labs/semver-multi


(ns semver-multi.common.git
  (:require [clojure.string    :as str]
            [babashka.process  :refer [shell]]))



(defn ^:impure get-git-root-dir
  "Returns the absolute directory path as a string to the git root directory or 'nil' if the command was not executed in
   a git repo or the command failed."
  []
  (let [resp (-> (shell {:out :string :err :string :continue true} "git rev-parse --show-toplevel")
                 (select-keys [:out :err]))]
    (if (empty? (:out resp))
      nil
      (str/trim (:out resp)))))


(defn ^:impure get-git-branch
  "Returns the branch name active in the repo or 'nil' if the command was not executed in a git repo or the command
   failed."
  []
  (let [resp (-> (shell {:out :string :err :string :continue true} "git rev-parse --abbrev-ref HEAD")
                 (select-keys [:out :err]))]
    (if (empty? (:out resp))
      nil
      (str/trim (:out resp)))))
#!/usr/bin/env bb

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


(ns semver-multi.common.util
  (:import (java.util.regex Pattern)))



(defn do-on-success
  "Perfroms the function 'fn' if the last argument is a map with key 'success'set to 'true', otherwise returns the last
   argument."
  ([fn arg]
   (if (:success arg)
     (fn arg)
     arg))
  ([fn arg1 arg2]
   (if (:success arg2)
     (fn arg1 arg2)
     arg2))
  ([fn arg1 arg2 arg3]
   (if (:success arg3)
     (fn arg1 arg2 arg3)
     arg3))
  ([fn arg1 arg2 arg3 arg4]
   (if (:success arg4)
     (fn arg1 arg2 arg3 arg4)
     arg4)))


;; todo:  account for build info
(defn is-semantic-version-release?
  "Returns 'true' if `version` is a valid semantic version for a release and 'false' otherwise."
  [version]
  (if (seq (re-find (Pattern/compile "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$") version))
    true
    false))
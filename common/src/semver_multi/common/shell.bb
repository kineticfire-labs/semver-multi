#!/usr/bin/env bb

;; (c) Copyright 2024-2025 KineticFire. All rights reserved.
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


(ns semver-multi.common.shell
  (:require [clojure.string    :as str]
            [babashka.process  :refer [shell]]))



(def ^:const shell-color-red "\\e[1m\\e[31m")

(def ^:const shell-color-yellow "\\e[1m\\e[33m")

(def ^:const shell-color-blue "\\e[34m")

(def ^:const shell-color-white "\\e[0m\\e[1m")

(def ^:const shell-color-reset "\\033[0m\\e[0m")




(defn ^:impure run-shell-command
  "Runs commands in 'lines', as either a string or vector of strings, by using 'shell'."
  [lines]
  (if (= (.getSimpleName (type lines)) "String")
    (run-shell-command [lines])
    (dorun (map shell lines))))


(defn apply-display-with-shell
  "Applies 'echo -e' to each line in 'lines', which supports display to the terminal with color coding, and returns the
   result.  If argument 'lines' is a string, then returns a string; if 'lines' is a collection of strings, then returns
   a lazy sequence of strings."
  [lines]
  (if (= (.getSimpleName (type lines)) "String")
    (str "echo -e " lines)
    (map #(str "echo -e " %) lines)))


(defn apply-display-with-shell-without-newline
  "Applies 'echo -n -e' to string 'line' and returns the string result.  The '-n' does not print a newline.  The '-e'
   enables display to the terminal with color coding, and returns the result."
  [line]
  (str "echo -n -e " line))


(defn apply-quotes
  "Applies quotes to `lines` and returns the result.  If argument 'lines' is a string, then returns a string; if 'lines'
   is a collection of strings, then returns a lazy sequence of strings."
  [lines]
  (if (= (.getSimpleName (type lines)) "String")
    (str "\"" lines "\"")
    (map #(str "\"" % "\"") lines)))


(defn generate-shell-newline-characters
  "Generates newline characters understood by the terminal and returns the string result.  Displays one newline without
   arguments or int 'num' newlines."
  ([]
   (generate-shell-newline-characters 1))
  ([num]
   (str/join "" (repeat num "\n"))))
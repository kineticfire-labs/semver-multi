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


(ns semver-multi.common.commit
  (:require [clojure.string                   :as str]
            [semver-multi.common.shell       :as cshell]
            [semver-multi.common.system      :as system]
            [semver-multi.common.string      :as cstr]
            [semver-multi.common.collections :as col]
            [semver-multi.common.project-def :as proj])
  (:import (java.util.regex Pattern)))


;; this namespace provides functionality to:
;;    - re-write (re-format) a commit message
;;    - generate a commit message success or error message 
;;    - validate a commit message


;;
;; section: re-write (re-format) a commit message
;;

(defn format-commit-msg-all
  "Performs overall formatting of the commit message--what can be applied to the entire message--with the message as a
   multi-line string 'commit-msg' and returns the formatted multi-line string as the result."
  [commit-msg]
  (-> commit-msg
      (str/replace #"(?m)^.*#.*" "")                                            ;; replace all lines that contain comments with empty strings
      (str/trim)                                                                ;; remove leading/trailing newlines/spaces
      (str/replace #"(?m)^[ ]+$" "")                                            ;; for a line with spaces only, remove all spaces
      (str/replace #"(?m)^\n{2,}" "\n")                                         ;; replace two or more consecutive newlines with a single newline
      (str/replace #"(?m)[ ]+$" "")                                            ;; remove spaces at end of lines (without removing spaces at beginning of lines)
      (str/replace #"^(.+)\n+(.)" "$1\n\n$2")                                   ;; ensure exactly two newlines between subject and body (if any body)
      (str/replace #"(?mi)BRE?AKING[ -_]*CHANGE[ ]*:[ ]*" "BREAKING CHANGE: ")  ;; convert to 'BREAKING CHANGE:' regardless of: case, mispelled 'BRAKING', separated with space/dash/underscore, and searpated by 0 or more spaces before and/or after the colon
      (str/replace #"(?mi)BRAEKING[ -_]*CHANGE[ ]*:[ ]*" "BREAKING CHANGE: ")   ;; as above, if mispelled 'BRAEKING'
      (str/trim)))                                                              ;; remove leading/trailing newlines/spaces (again)


(defn format-commit-msg-first-line
  "Performs formatting of the first line (e.g. subject line aka title line) only of the commit message and returns the
   formatted string result.  The 'line' must be a string of the first line only."
  [line]
  (-> line
      (str/trim)                     ;; remove spaces at beginning/end of line
      (str/replace #"[ ]*\(" "(")    ;; remove extra spaces before the opening parenthesis
      (str/replace #"\([ ]*" "(")    ;; remove extra spaces after the opening parenthesis
      (str/replace #"[ ]*\)" ")")    ;; remove extra spaces before the closing parenthesis
      (str/replace #"\)[ ]*" ")")    ;; remove extra spaces after the closing parenthesis
      (str/replace #"[ ]*!" "!")     ;; remove extra spaces before the exclamation mark
      (str/replace #"[ ]*:" ":")     ;; remove extra spaces before the colon
      (str/replace #":[ ]*" ": ")))  ;; replace no space or extra spaces after the colon with a single space


(defn format-commit-msg
  "Accepts a string commit-msg and returns the formatted string commit-message.  If the commit message is an empty
   string or nil, then returns an empty string."
  [commit-msg]
  (if (empty? commit-msg)
    ""
    (let [commit-msg-vec (cstr/split-lines (format-commit-msg-all commit-msg))]
      (str/join "\n" (into [] (concat (conj [] (format-commit-msg-first-line (first commit-msg-vec))) (rest commit-msg-vec)))))))

;;
;; section: generate a commit message success or error message 
;;

(defn generate-commit-msg-offending-line-header
  "Generates a header that indicates an offending line that was in error, if sequence 'lines-num' is non-nil and
   non-empty; indexes in 'lines-num' are indexed starting at 0.  Appends the header line to the vector of strings
   'lines' and returns the result or, if no header should be generated, returns 'lines' unchanged."
  [lines lines-num]
  (if (empty? lines-num)
    lines
    (conj lines (str "\"   offending line(s) # " (pr-str (map inc lines-num)) " in red **************\""))))


(defn generate-commit-msg-offending-line-msg-highlight
  "Adds shell color-code formatting for an offending line(s) identified in the 'lines-num' sequence to the vector of
   strings 'lines'.  Contents of 'lines-num' are integer indicies indexed starting at 0.  If 'lines-num' is 'nil' or
   empty, then 'lines' is returned unchanged."
  [lines lines-num]
  (if (empty? lines-num)
    lines
    (vec (map-indexed (fn [idx line] (if (some (fn [num] (= idx num)) lines-num)
                                       (str cshell/shell-color-red line cshell/shell-color-reset)
                                       line)) lines))))

(defn generate-commit-msg
  "Generates a formatted commit message, using string 'msg', with optional call-out to the offending line(s) if the
   'lines-num' sequence is non-nil and non-empty; 'lines-num' is indexed starting at 0.  Returns the result as a lazy
   sequence of strings, formatted for shell output with color-coding."
  ([msg]
   (generate-commit-msg msg nil))
  ([msg lines-num]
   (let [msg-vec (cstr/split-lines msg)
         start-lines-top
         [(str "\"" cshell/shell-color-blue "**********************************************\"")
          "\"BEGIN - COMMIT MESSAGE ***********************\""]
         start-line-end
         (str "\"**********************************************" cshell/shell-color-reset "\"")
         end-lines
         [(str "\"" cshell/shell-color-blue "**********************************************\"")
          "\"END - COMMIT MESSAGE *************************\""
          (str "\"**********************************************" cshell/shell-color-reset "\"")]]
     (cshell/apply-display-with-shell
      (into (into (conj (generate-commit-msg-offending-line-header start-lines-top lines-num) start-line-end) (generate-commit-msg-offending-line-msg-highlight msg-vec lines-num)) end-lines)))))


(defn generate-commit-err-msg
  "Generates and returns as a vector of strings an error message including the string 'title' as part of the title and
   the string 'err-msg' as the reason, formatting the string for shell output with color-coding."
  [title err-msg]
  (cshell/apply-display-with-shell
   [(str "\"" cshell/shell-color-red "COMMIT REJECTED " title "\"")
    (str "\"" cshell/shell-color-red "Commit failed reason: " err-msg cshell/shell-color-reset "\"")]))


;; todo: rename specific to commit?
(defn ^:impure handle-err
  "Generates and displays to the shell an error message, including the string 'title' as part of the title and the
   string 'err-msg' as the reason, using color-coding from the shell.  Optionally accepts a string 'commit-msg' to
   display; and optionally accepts a sequence 'line-num' of integer line numbers, indexed at 0, which displays a message
   about the offending line and highlights it in the commit message or can be 'nil'.  Exits with return code 1."
  ([title err-msg]
   (cshell/run-shell-command (generate-commit-err-msg title err-msg))
   (system/exit-now! 1))
  ([title err-msg commit-msg]
   (cshell/run-shell-command (generate-commit-err-msg title err-msg))
   (cshell/run-shell-command (generate-commit-msg commit-msg))
   (system/exit-now! 1))
  ([title err-msg commit-msg line-num]
   (cshell/run-shell-command (generate-commit-err-msg title err-msg))
   (cshell/run-shell-command (generate-commit-msg commit-msg line-num))
   (system/exit-now! 1)))


(defn generate-commit-warn-msg
  "Generates and returns as a string a warning message including the string 'title' as part of the title and 'warn-msg'
   as the reason, formatting the string for shell output with color-coding."
  [title warn-msg]
  (cshell/apply-display-with-shell
   [(str "\"" cshell/shell-color-yellow "COMMIT WARNING " title "\"")
    (str "\"" cshell/shell-color-yellow "Commit proceeding with warning: " warn-msg cshell/shell-color-reset "\"")]))


;; todo: rename specific to commit?
(defn ^:impure handle-warn-proceed
  "Generates and displays to the terminal a warning message, including the string 'title' as part of the title and
   'warn-msg' as the reason, using color-coding from the shell.  Exits with return code 0."
  [title warn-msg]
  (cshell/run-shell-command (generate-commit-warn-msg title warn-msg))
  (system/exit-now! 0))

;; todo: rename specific to commit?
(defn ^:impure handle-ok
  "Displays a 'success' message including string `title` for a valid commit edit message and exits with return value 0."
  [title]
  (cshell/run-shell-command (cshell/apply-display-with-shell [(str "\"" cshell/shell-color-white "Commit ok, per " title "\"")]))
  (system/exit-now! 0))


(defn create-validate-commit-msg-err
  "Creates and return a map describing a commit message validation error with key 'success' to 'false', 'reason', and
   optional 'locations'."
  ([reason]
   (create-validate-commit-msg-err reason nil))
  ([reason locations]
   (let [response (-> {}
                      (assoc :success false)
                      (assoc :reason reason))]
     (if (nil? locations)
       response
       (assoc response :locations locations)))))


;;
;; section: validate a commit message
;;

(defn validate-commit-msg-title-len
  "Validates the commit message string 'title' (e.g. first line), returning 'nil' on success and a map on error with key
   'success' equal to 'false', 'reason', and optional 'locations'.  The title is valid if it's within the min/max
   character range (inclusive) set in the config file."
  [title config]
  (if (seq (re-find (Pattern/compile (str "^.{" (:min (:title-line (:length (:commit-msg config)))) ",}$")) title))    ;; regex for n or more characters
    (if (seq (re-find (Pattern/compile (str "^.{1," (:max (:title-line (:length (:commit-msg config)))) "}$")) title)) ;; regex for n or fewer characters
      nil
      (create-validate-commit-msg-err (str "Commit message title line must not contain more than " (:max (:title-line (:length (:commit-msg config)))) " characters.") (lazy-seq [0])))
    (create-validate-commit-msg-err (str "Commit message title line must be at least " (:min (:title-line (:length (:commit-msg config)))) " characters.") (lazy-seq [0]))))


(defn validate-commit-msg-body-len
  "Validates the commit message 'body' (e.g. lines after the title) where each line of the body is an element of a
   vector; must not have an element representing the two newlines separating the title from the body. Returns 'nil' on
   success (including if 'body' is an empty sequence) and a map on error with key 'success' equal to 'false', 'reason',
   and optional 'locations'.  The body is valid if all lines are within the min/max character range (inclusive) set in
   the config file."
  [body config]
  (if (empty? body)
    nil
    (let [err-body-min (col/index-matches body (Pattern/compile (str "^.{1," (dec (:min (:body-line (:length (:commit-msg config))))) "}$")))]     ;; regex for n or more characters
      (if (= 0 (count err-body-min))
        (let [err-body-max (col/index-matches body (Pattern/compile (str "^.{" (inc (:max (:body-line (:length (:commit-msg config))))) ",}$")))]  ;; regex for n or fewer characters
          (if (= 0 (count err-body-max))
            nil
            (create-validate-commit-msg-err (str "Commit message body line must not contain more than " (:max (:body-line (:length (:commit-msg config)))) " characters.") err-body-max)))
        (create-validate-commit-msg-err (str "Commit message body line must be at least " (:min (:body-line (:length (:commit-msg config)))) " characters.") err-body-min)))))


(defn validate-commit-msg-title-scope-type
  "Validates the commit message title line (as a string) for type, scope, and description but does NOT check type/scope
   against the config.  Returns a map result of key 'success' set to bool 'true' with string 'type', string 'scope',
   bool 'breaking' if breaking change or not, and string 'title-descr'.  Else returns key 'success' set to bool 'false'
   with string 'reason'."
  [title]
  (let [matcher (re-matcher #"^(?<wip>~)?(?<type>[a-z]+)\((?<scope>([a-zA-Z0-9]+(.[a-zA-Z0-9]+)*))\)(?<breaking>!)?:(?<descr>.*)" title)]
    (if (.matches matcher)
      (let [match {:wip (if (empty? (.group matcher "wip"))
                          false
                          true)
                   :type (.group matcher "type")
                   :scope (.group matcher "scope")
                   :breaking (if (empty? (.group matcher "breaking"))
                               false
                               true)
                   :title-descr (str/trim (.group matcher "descr"))}
            reason (-> ""
                       (col/add-string-if-key-empty "Could not identify type." :type match)
                       (col/add-string-if-key-empty "Could not identify scope." :scope match)
                       (col/add-string-if-key-empty "Could not identify description." :title-descr match))]
        (if (empty? reason)
          (assoc match :success true)
          (create-validate-commit-msg-err (str "Bad form on title.  " reason) (lazy-seq [0]))))
      (create-validate-commit-msg-err "Bad form on title.  Could not identify type, scope, or description." (lazy-seq [0])))))


(defn validate-commit-msg
  "Valides the `commit-msg` using definitions in `config` and returns a map result.  If valid, returns key 'success' to
   boolean 'true', 'scope-path' as a vector of one or more string scopes, 'json-path' as a vector of one or more
   keywords or integer indicies through the config, 'type' as the string type of change, and 'breaking' as a boolean
   'true' if a breaking chagne and 'false' otherwise.  Else returns 'success' as boolean 'false', a string 'reason' for
   the error, and a lazy sequence 'locations' indicating the line number where the error occurred if applicable.  Valid
   if the `commit-msg` is not nil or empty, does not contain tabs, the title line and body lines are within the min/max
   character range per `config`, the title line meets the format for the type/scope/description and breaking change, and
   type/scope combination is defined in `config`; does not check footer tokens.  The `commit-msg` must be formatted and
   the `config` must be valid."
  [commit-msg config]
  (if (empty? commit-msg)
    (create-validate-commit-msg-err "Commit message cannot be empty.")
    (let [commit-msg-all-col (cstr/split-lines commit-msg)
          commit-msg-title (first commit-msg-all-col)
          commit-msg-body-col (rest (rest commit-msg-all-col))  ;; the two 'rest' operations get the body collection without the empty string created by the two newlines separating the title from the body, if there is a body
          err-tab-seq (col/index-matches commit-msg-all-col #"	")]
      (if (= 0 (count err-tab-seq))
        (let [err-title (validate-commit-msg-title-len commit-msg-title config)]
          (if (nil? err-title)
            (let [err-body (validate-commit-msg-body-len commit-msg-body-col config)]
              (if (nil? err-body)
                (let [scope-type-response (validate-commit-msg-title-scope-type commit-msg-title)]
                  (if (:success scope-type-response)
                    (let [scope-path-response (proj/find-scope-path (:scope scope-type-response) config)]
                      (if (:success scope-path-response)
                        (let [types (get-in config (conj (:json-path scope-path-response) :types))]
                          (if (some (fn [itm] (= (:type scope-type-response) itm)) types)
                            {:success true
                             :scope-path (:scope-path scope-path-response)
                             :json-path (:json-path scope-path-response)
                             :wip (:wip scope-type-response)
                             :type (:type scope-type-response)
                             :breaking (or (:breaking scope-type-response) (if (> (count (col/index-matches commit-msg-all-col #"BREAKING CHANGE:")) 0)
                                                                             true
                                                                             false))}
                            (create-validate-commit-msg-err (str "Definition in title line of type '" (:type scope-type-response) "' for scope '" (:scope scope-type-response) "' at query path of '" (:json-path scope-path-response) "' not found in config.") (lazy-seq [0]))))
                        scope-path-response))
                    scope-type-response))
                err-body))
            err-title))
        (create-validate-commit-msg-err "Commit message cannot contain tab characters." err-tab-seq)))))



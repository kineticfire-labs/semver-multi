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


(ns semver-multi.common.project-def
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [semver-multi.common.collections :as col]
            [semver-multi.common.util :as util]))


;; This namespace provides functionality to:
;;    - query the project definition
;;    - manipulate config items
;;    - validate config, which generates an 'enhanced' config


(def ^:const default-project-def-file "semver-multi.json")

(def ^:const types-allowed-fields [:description
                                   :triggers-build
                                   :version-increment
                                   :direction-of-change
                                   :num-scopes])

(def ^:const types-version-increment-allowed-values [:minor :patch])

(def ^:const types-direction-of-change-allowed-values [:up :down])

(def ^:const non-editable-default-types [:revert :merge])

;; the behavior of 'revert' and 'merge' cannot be changed
(def ^:const default-types
  {
    :revert {
             :description "Revert to a previous commit version"
             }
    :merge {
            :description "Merge one branch into another"
            }
    :feat {
           :description "Add a new feature"
           :triggers-build true
           :version-increment :minor
           :direction-of-change :up
           :num-scopes [1]
           }
    :more {
           :description "Add code for a future feature (later indicated as complete with 'feat'). Support branch abstraction."
           :triggers-build true
           :version-increment :patch
           :direction-of-change :up
           :num-scopes [1]
           }
    :change {
             :description "Change implementation of existing feature"
             :triggers-build true
             :version-increment :patch
             :direction-of-change :up
             :num-scopes [1]
             }
    :remove {
             :description "Remove a feature"
             :triggers-build true
             :version-increment :minor
             :direction-of-change :up
             :num-scopes [1]
             }
    :less {
           :description "Remove code for a feature (already indicated as removed with 'remove'). Support branch abstraction."
           :triggers-build true
           :version-increment :patch
           :direction-of-change :up
           :num-scopes [1]
           }
    :deprecate {
                :description "Indicate some code is deprecated"
                :triggers-build true
                :version-increment :patch
                :direction-of-change :up
                :num-scopes [1]
                }
    :fix {
          :description "Fix a defect (e.g., bug)"
          :triggers-build true
          :version-increment :patch
          :direction-of-change :up
          :num-scopes [1]
          }
   :clean {
         :description "Clean-up code"
         :triggers-build false
         :version-increment :patch
         :direction-of-change :up
         :num-scopes [1]
         }
    :refactor {
               :description "Rewrite and/or restructure code without changing behavior. Could affect two scopes."
               :triggers-build false
               :version-increment :patch
               :direction-of-change :up
               :num-scopes [1 2]}
    :struct {
             :description "Project structure, e.g. directory layout. Could affect two scopes."
             :triggers-build true
             :version-increment :patch
             :direction-of-change :up
             :num-scopes [1 2]}
    :perf {
           :description "Improve performance, as a special case of refactor"
           :triggers-build true
           :version-increment :minor
           :direction-of-change :up
           :num-scopes [1]
           }
    :security {
               :description "Improve security aspect"
               :triggers-build true
               :version-increment :minor
               :direction-of-change :up
               :num-scopes [1]
               }
    :style {
            :description "Does not affect the meaning or behavior"
            :triggers-build false
            :version-increment :patch
            :direction-of-change :up
            :num-scopes [1]
            }
    :test {
           :description "Add or correct tests"
           :triggers-build false
           :version-increment :patch
           :direction-of-change :up
           :num-scopes [1]
           }
    :docs {
           :description "Affect documentation. Scope may affect meaning. When applied to 'code', affects API documentation (such as documentation for public and protected methods and classes with default javadocs)"
           :triggers-build false
           :version-increment :patch
           :direction-of-change :up
           :num-scopes [1]
           }
    :idocs {
            :description "Affect internal documentation that wouldn't appear in API documentation (such as comments and documentation for private methods with default javadocs)"
            :triggers-build false
            :version-increment :patch
            :direction-of-change :up
            :num-scopes [1]
            }
    :build {
            :description "Affect build components like the build tool"
            :triggers-build false
            :version-increment :patch
            :direction-of-change :up
            :num-scopes [1]
            }
    :vendor {
             :description "Update version for dependencies and packages"
             :triggers-build true
             :version-increment :patch
             :direction-of-change :up
             :num-scopes [1]
             }
    :ci {
         :description "Affect CI pipeline"
         :triggers-build false
         :version-increment :patch
         :direction-of-change :up
         :num-scopes [1]
         }
    :ops {
          :description "Affect operational components like infrastructure, deployment, backup, recovery, etc."
          :triggers-build true
          :version-increment :patch
          :direction-of-change :up
          :num-scopes [1]
          }
    :chore {
            :description "Miscellaneous commits, such as updating .gitignore"
            :triggers-build false
            :version-increment :patch
            :direction-of-change :up
            :num-scopes [1]}})


;;
;; section: query the project definition
;;


(defn config-enabled?
  [config]
  (if (:enabled (:commit-msg-enforcement config))
    true
    false))


;(defn get-scope-from-scope-or-alias
;  "Returns the scope as a string if either scope or scope-alias in `node` match the `scope-query` else nil.  The `node`
;   and `scope-query` must be valid.  The `node` can be a project or artifact."
;  [scope-query node]
;  (let [scope (:scope node)]
;    (if (= scope scope-query)
;      scope
;      (let [scope-alias (:scope-alias node)]
;        (if (= scope-alias scope-query)
;          scope
;          nil)))))
;
;
;(defn get-name
;  "Returns the node's name as a string if found else 'nil'.  Argument `node or `nodes` must be a valid config (map).
;   For `node`, returns the name at that location.  For `nodes`, returns the name at the path of `query` in `nodes`."
;  ([node]
;   (:name node))
;  ([nodes query]
;   (get-in nodes (conj query :name))))
;
;
;(defn get-scope
;  "Returns the node's scope as a string if found else 'nil'.  Argument `node or `nodes` must be a valid config (map).
;   For `node`, returns the scope at that location.  For `nodes`, returns the scope at the path of `query` in `nodes`."
;  ([node]
;   (:scope node))
;  ([nodes query]
;   (get-in nodes (conj query :scope))))
;
;
;(defn get-scope-alias-else-scope
;  "Returns the node's scope-alias, if defined, else returns the scope as a string; if neither are found, returns 'nil'.
;   Argument `node or `nodes` must be a valid config (map).  For `node`, returns the scope alias else scope at that
;   location.  For `nodes`, returns the scope alias else scope at the path of `query` in `nodes`."
;  ([node]
;   (get-scope-alias-else-scope node []))
;  ([node query]
;   (let [full-query (conj query :scope-alias)
;         scope-alias (get-in node full-query)]
;     (if (not (nil? scope-alias))
;       scope-alias
;       (get-scope node query)))))
;
;
;(defn get-scope-in-col
;  "Searches for the `scope-query` in the collection `col` of maps, where the query could be found in ':scope' or
;   ':scope-alias'.  Returns a map on success with key 'success' set to 'true', 'scope' set to the scope found even if
;   the match was to a scope-alias, and 'index' as the zero-based index of the match in the collection.  Returns 'nil' if
;   a match is not found."
;  [scope-query col]
;  (let [result (keep-indexed (fn [idx itm]
;                               (let [result (get-scope-from-scope-or-alias scope-query itm)]
;                                 (if (nil? result)
;                                   nil
;                                   {:success true
;                                    :scope result
;                                    :index idx}))) col)]
;    (if (empty? result)
;      {:success false}
;      (first result))))
;
;
;(defn get-scope-in-artifacts-or-projects
;  "Finds the string `scope`, which can be a scope or scope alias, in the `node` ':artifacts' or ':projects' and returns
;   a map result.  If found, returns key 'success' to boolean 'true', 'scope' to the string scope (even if the input
;   scope was a scope alias), the `property` where the scope was found as either keyword ':artifacts' or ':projects', and
;   `index` to the zero-based index in the sequence.  Otherwise, returns boolean 'false'.  The `scope` and `node` must be
;   valid."
;  [scope node]
;  (let [artifact-result (get-scope-in-col scope (:artifacts node))]
;    (if (:success artifact-result)
;      {:success true
;       :scope (:scope artifact-result)
;       :property :artifacts
;       :index (:index artifact-result)}
;      (let [project-result (get-scope-in-col scope (:projects node))]
;        (if (:success project-result)
;          {:success true
;           :scope (:scope project-result)
;           :property :projects
;           :index (:index project-result)}
;          {:success false})))))
;
;
;(defn find-scope-path
;  "Finds the scope and json paths for the string `query-path`, which can be a dot-separated path of scope and/or
;   scope-aliases, using the `config` returning a map result.  If found, returns key 'success' to boolean 'true',
;   'scope-path' as a vector of strings of scopes (even if the `query-path` contained scope aliases), and the 'json-path'
;   as a vector of the json path (using keywords and integer indices) through the config.  Else if invalid, then returns
;   'success' to boolean 'false', a 'scope-or-alias' set the scope or scope-alias that failed, and a 'query-path' of the
;   full query path that failed.  The `config` must be valid."
;  [query-path config]
;  (let [query-path-vec-top (str/split query-path #"\.")
;        scope-top (first query-path-vec-top)
;        node-top (get-in config [:project])
;        root-project-scope (get-scope-from-scope-or-alias scope-top node-top)]  ;; check top-level project outside of loop, since it's json path is ':project' singluar vs ':projects' plural for artifacts/sub-projects
;    (if (nil? root-project-scope)
;      ;; todo: this returned: (str "Definition for scope or scope-alias in title line of '" scope-top "' at query path of '[:project]' not found in config.") with 'locations' as a seq with 0
;      {:success false
;       :scope-or-alias scope-top
;       :query-path [:project]}
;      (loop [scope-path [root-project-scope]           ;; the scope path that has been found thus far
;             json-path [:project]                      ;; the path to the current node, which consists of map keys and/or array indicies
;             query-path-vec (rest query-path-vec-top)  ;; the query path of scopes/scope-aliases that need to resolved
;             node node-top]                            ;; the current node on which to find the next scope, from 'artifacts' or 'projects'
;        (if (= 0 (count query-path-vec))
;          {:success true
;           :scope-path scope-path
;           :json-path json-path}
;          (let [scope (first query-path-vec)
;                result (get-scope-in-artifacts-or-projects scope node)]
;            (if (:success result)
;              (let [next-json-path (conj json-path (:property result) (:index result))]
;                (recur (conj scope-path (:scope result)) next-json-path (rest query-path-vec) (get-in config next-json-path)))
;              ;; todo: this returned: (str "Definition for scope or scope-alias in title line of '" scope "' at query path of '" (conj json-path [:artifacts :projects]) "' not found in config.") with 'locations' as a seq with 0
;              {:success false
;               :scope-or-alias scope
;               :query-path (conj json-path [:artifacts :projects])})))))))
;
;
;(defn get-child-nodes
;  "Returns vector of child node descriptions as maps (projects and/or artifacts) for the `node` or an empty vector if
;    there are no child nodes.  The child node descriptions are built from the `child-node-descr` and `parent-path`."
;  [node child-node-descr parent-path]
;  (into [] (reverse (concat
;                     (map-indexed
;                      (fn [idx itm] (assoc child-node-descr :json-path (conj parent-path :artifacts idx))) (get-in node [:artifacts]))
;                     (map-indexed
;                      (fn [idx itm] (assoc child-node-descr :json-path (conj parent-path :projects idx))) (get-in node [:projects]))))))
;
;
;(defn get-depends-on
;  "Returns a vector of 'depends-on' node descriptions as maps for the `node`.  Returns an empty vector if there are no
;   'depends-on' nodes."
;  [node config]
;  (let [depends-on-scope-paths-formatted (get-in node [:depends-on])]
;    (if (empty? depends-on-scope-paths-formatted)
;      []
;      (into [] (map
;                (fn [itm] (let [result (find-scope-path itm config)]
;                            {:json-path (:json-path result)
;                             :scope-path (:scope-path result)})) depends-on-scope-paths-formatted)))))
;
;
;(defn get-child-nodes-including-depends-on
;  "Returns a vector of child node descriptions, including 'depends-on', or an empty vector if there are no child nodes.
;   Requires an enhanced config where each project and artifact has defined :full-json-path, :full-scope-path, and
;   :full-scope-path-formatted."
;  [node config]
;  (into [] (concat
;
;            (map
;             (fn [itm] {:full-json-path (:full-json-path itm)
;                        :full-scope-path (:full-scope-path itm)
;                        :full-scope-path-formatted (:full-scope-path-formatted itm)}) (get-in node [:artifacts]))
;
;            (map
;             (fn [itm] {:full-json-path (:full-json-path itm)
;                        :full-scope-path (:full-scope-path itm)
;                        :full-scope-path-formatted (:full-scope-path-formatted itm)}) (get-in node [:projects]))
;
;            (map
;             (fn [itm] (let [cur-node (get-in config (:json-path itm))]
;                         {:full-json-path (:full-json-path cur-node)
;                          :full-scope-path (:full-scope-path cur-node)
;                          :full-scope-path-formatted (:full-scope-path-formatted cur-node)})) (get-depends-on node config)))))
;
;
;(defn get-all-scopes-from-collection-of-artifacts-projects
;  "Returns a vector of all scopes at the path `json-path-vector` in config `config`, where `json-path-vector` is
;   a vector that may define a location to a collection of artifacts or projects.  If no vector containing maps with
;   key 'scope' exists, then an empty vector is returned."
;  [config json-path-vector]
;  (vec (map (fn [itm] (:scope itm)) (get-in config json-path-vector))))
;
;
;(defn get-full-scope-paths
;  "Returns a vector of full scope path vectors formed from combining the `scope-path-vector` with each scope in
;   `scope-vector`.  If `scope-vector` is empty, then an empty vector is returned."
;  [scope-path-vector scope-vector]
;  (if (empty? scope-vector)
;    []
;    (vec (map (fn [itm] (conj scope-path-vector itm)) scope-vector))))
;
;
;(defn get-all-full-scopes
;  "Returns a vector of all full scopes found in the `config`.  The first element of the returned vector is always the
;   root project.  The config must valid."
;  [config]
;  (loop [all-scopes-vector []
;         to-visit-stack [{:parent-scope-path-vector []
;                          :json-path-vector [:project]}]]
;    (if (empty? to-visit-stack)
;      all-scopes-vector
;      (let [current-node (last to-visit-stack)
;            to-visit-stack (vec (pop to-visit-stack))
;            parent-scope-path-vector (:parent-scope-path-vector current-node)
;            json-path-vector (:json-path-vector current-node)
;            project-scope (get-in config (conj json-path-vector :scope))
;            scope-path-vector (vec (first (get-full-scope-paths parent-scope-path-vector [project-scope])))
;            all-scopes-vector (vec (-> all-scopes-vector
;                                       ;; add project scope
;                                       (concat [scope-path-vector])
;                                       ;; add artifacts' scopes
;                                       (concat (get-full-scope-paths (conj parent-scope-path-vector project-scope) (get-all-scopes-from-collection-of-artifacts-projects config (conj json-path-vector :artifacts))))))
;            projects-scopes-vector (get-all-scopes-from-collection-of-artifacts-projects config (conj json-path-vector :projects))
;            found-nodes (vec (map-indexed (fn [idx itm] {:parent-scope-path-vector scope-path-vector
;                                                         :json-path-vector (conj json-path-vector :projects idx)}) projects-scopes-vector))
;            to-visit-stack (if (> (count found-nodes) 0)
;                             (vec (concat to-visit-stack (reverse found-nodes)))
;                             to-visit-stack)]
;        (recur all-scopes-vector to-visit-stack)))))
;
;
;;;
;;; section: manipulate config items
;;;
;
;;; does this one duplicate 'scope-list-to-string'?
;(defn create-scope-string-from-vector
;  "Returns a scope string from the scope vector `scope-vector`."
;  [scope-vector]
;  (clojure.string/join "." scope-vector))
;
;
;(defn scope-list-to-string
;  "Converts a scope list `scopes` (that is, a full scope represented by a  list of strings) into a dot-separated full
;   scope.  The argument `scopes` may be a list representing a full scope or a list of multiple scope lists."
;  [scopes]
;  (let [first-element (first scopes)]
;    (if (nil? first-element)
;      scopes
;      (if (coll? first-element)
;        (map (fn [itm] (scope-list-to-string itm)) scopes)
;        (str/join "." scopes)))))


;;
;; section: validate config
;;


(defn validate-config-fail
  "Returns a map with key ':success' with value boolean 'false' and ':reason' set to string 'msg'.  If map 'data' is
   given, then associates the map values into 'data'."
  ([msg]
   {:success false :reason msg})
  ([msg data]
   (-> data
       (assoc :success false)
       (assoc :reason msg))))


(defn validate-config-version
  "Returns 'true' if the 'version' field in the config is a valid semantic version for '<major>.<minor>.<patch>' and
  'false' otherwise."
  [data]
  (if-not (contains? (:config data) :version)
    (validate-config-fail "Version field 'version' is required" data)
    (let [version (get-in data [:config :version])]
      (if-not (util/valid-string? false 1 Integer/MAX_VALUE version)
        (validate-config-fail "Version field 'version' must be a non-empty string" data)
        (if-not (util/is-semantic-version-release? version)
          (validate-config-fail "Version field 'version' must be a valid semantic version release" data)
          (assoc data :success true))))))


(defn validate-config-msg-enforcement
  "Validates the 'commit-msg-enforcement' fields in the config at key 'config' in map 'data'.  Returns map 'data' with
   key ':success' set to boolean 'true' if valid or boolean 'false' and ':reason' set to a string message."
  [data]
  (let [enforcement (get-in data [:config :commit-msg-enforcement])
        enabled (:enabled enforcement)]
    (if (some? enforcement)
      (if (nil? enabled)
        (validate-config-fail "Commit message enforcement must be set as enabled or disabled (commit-msg-enforcement.enabled) with either 'true' or 'false'." data)
        (if (boolean? enabled)
          (assoc data :success true)
          (validate-config-fail "Commit message enforcement 'enabled' (commit-msg-enforcement.enabled) must be a boolean 'true' or 'false'." data)))
      (validate-config-fail "Commit message enforcement block (commit-msg-enforcement) must be defined." data))))


(defn validate-config-commit-msg-length
  "Validates the min and max length fields in the config at key 'config' in map 'data'.  Returns map 'data' with key
   ':success' set to boolean 'true' if valid or boolean 'false' and ':reason' set to a string message."
  [data]
  (let [title-line-min (get-in data [:config :commit-msg :length :title-line :min])
        title-line-max (get-in data [:config :commit-msg :length :title-line :max])
        body-line-min (get-in data [:config :commit-msg :length :body-line :min])
        body-line-max (get-in data [:config :commit-msg :length :body-line :max])]
    (if (some? title-line-min)
      (if (some? title-line-max)
        (if (some? body-line-min)
          (if (some? body-line-max)
            (if (pos-int? title-line-min)
              (if (pos-int? title-line-max)
                (if (>= title-line-max title-line-min)
                  (if (pos-int? body-line-min)
                    (if (pos-int? body-line-max)
                      (if (>= body-line-max body-line-min)
                        (assoc data :success true)
                        (validate-config-fail "Maximum length of body line (length.body-line.max) must be equal to or greater than minimum length of body line (length.body-line.min)." data))
                      (validate-config-fail "Maximum length of body line (length.body-line.max) must be a positive integer." data))
                    (validate-config-fail "Minimum length of body line (length.body-line.min) must be a positive integer." data))
                  (validate-config-fail "Maximum length of title line (length.title-line.max) must be equal to or greater than minimum length of title line (length.title-line.min)." data))
                (validate-config-fail "Maximum length of title line (length.title-line.max) must be a positive integer." data))
              (validate-config-fail "Minimum length of title line (length.title-line.min) must be a positive integer." data))
            (validate-config-fail "Maximum length of body line (length.body-line.max) must be defined." data))
          (validate-config-fail "Minimum length of body line (length.body-line.min) must be defined." data))
        (validate-config-fail "Maximum length of title line (length.title-line.max) must be defined." data))
      (validate-config-fail "Minimum length of title line (length.title-line.min) must be defined." data))))


(defn validate-config-release-branches
  "Validates the 'release-branches' field.  To be valid, the field must:
     - exist
     - be a collection (non-nil)
     - contain 1 to Integer/MAX_VALUE elements (inclusive)
     - not contain duplicates
     - each element must
        - be a string (no nil values)
        - be 1 to Integer/MAX_VALUE in length (no empty strings)

   If valid, updates ':config' in data by converting the strings in 'release-branches' to keywords, and returns the
   updated map 'data' with key ':success' set to boolean 'true'.  If invalid, ':success' is set to 'false' and 'reason'
   is set to a string message."
  [data]
  (if (util/valid-map-entry? [:config :release-branches] true false
                             (partial util/valid-coll? false 1 Integer/MAX_VALUE
                                      (partial util/valid-string? false 1 Integer/MAX_VALUE))
                             data)
    (assoc
      (update-in data [:config :release-branches] (partial mapv keyword))
      :success true)
    (validate-config-fail "Property 'release-branches' must be defined as an array of one or more non-empty strings.")))


(defn validate-if-present
  "Returns the result of the function `fn` if the field `field` is contained the map `type-map`.  If the field is not
  contained in the map, then returns `true`."
  [type-map field fn]
  (if-not (contains? type-map field)
    true
    (fn)))


(defn validate-version-increment
  "Validates the ':version-increment' field in the map `type-map`.  If valid, returns a map with key ':success' to true;
  if the ':version-increment' field was set, then returns that field with the value changed to a keyword else not set.
  If invalid, then ':success' is false and key ':fail-point' indicates the reason for the failure with
  ':version-increment-format' (the value is invalid) or ':version-increment-allowed' (the value is not in the allowed
  values).  The `type-map` must be a map (not nil).

  To be valid, the field must:
    - not be set, or if set:
    - be a string that converts to a keyword in 'types-version-increment-allowed-values'
  "
  [type-map]
  (if-not (contains? type-map :version-increment)
    {:success true}
    (if-not (util/valid-string? false 1 Integer/MAX_VALUE (:version-increment type-map))
      {:success false
       :fail-point :version-increment-format}
      (let [version-increment-keyword (keyword (:version-increment type-map))
            diff-version-increment (vec (set/difference #{version-increment-keyword} (set types-version-increment-allowed-values)))]
        (if (> (count diff-version-increment) 0)
          {:success false
           :fail-point :version-increment-allowed}
          {:success true
           :version-increment version-increment-keyword})))))


(defn validate-direction-of-change
  "Validates the ':direction-of-change' field in the map `type-map`.  If valid, returns a map with key ':success' to
  true; if the ':direction-of-change' field was set, then returns that field with the value changed to a keyword else
  not set. If invalid, then ':success' is false and key ':fail-point' indicates the reason for the failure with
  ':direction-of-change-format' (the value is invalid) or ':direction-of-change-allowed' (the value is not in the
  allowed values).  The `type-map` must be a map (not nil).

  To be valid, the field must:
    - not be set, or if set:
    - be a string that converts to a keyword in 'types-direction-of-change-allowed-values'
  "
  [type-map]
  (if-not (contains? type-map :direction-of-change)
    {:success true}
    (if-not (util/valid-string? false 1 Integer/MAX_VALUE (:direction-of-change type-map))
      {:success false
       :fail-point :direction-of-change-format}
      (let [direction-of-change-keyword (keyword (:direction-of-change type-map))
            diff-direction-of-change (vec (set/difference #{direction-of-change-keyword} (set types-direction-of-change-allowed-values)))]
        (if (> (count diff-direction-of-change) 0)
          {:success false
           :fail-point :direction-of-change-allowed}
          {:success true
           :direction-of-change direction-of-change-keyword})))))


(defn validate-type-map
  "Checks the validity of a type-map, e.g. a single entry for either 'add' or 'update'.  If valid, returns key
  ':success' to true and key ':type-map' with the `type-map` including updates to it, if any.  If invalid, returns
  ':success' as false and keys ':fail-point' as the point of failure and, for some, a key ':offending-keys' for those
  keys that resulted in the failure.

  On success, updates to the type-map include:
    - :version-increment = converted to keywords
    - :direction-of-change = converted to keywords

  Valid:
    - if `must-contain-all-fields` is 'true', then all fields in 'types-allowed-fields' must be present
    - doesn't contain keys not defined in 'types-allowed-fields'
    - the fields, if set:
      - 'description is a string of length one character to Integer/MAX_VALUE (inclusive)
      - 'triggers-build' is a boolean
      - 'version-increment' is a String whose keyword is contained in 'types-version-increment-allowed-values'
      - 'direction-of-change' is a String whose keyword is contained in 'types-direction-of-change-allowed-values'
      - 'num-scopes' is a vector containing integers '1' or '2'
  "
  [type-map must-contain-all-fields]
  ;; check: if `must-contain-all-fields` is 'true', then all fields in 'types-allowed-fields' must be present
  (let [must-contain-fields-result (if must-contain-all-fields
                                     (let [diff-keys (vec (set/difference (set types-allowed-fields) (set (keys type-map))))]
                                       (if (> (count diff-keys) 0)
                                         {:success false
                                          :fail-point :required-keys
                                          :offending-keys diff-keys}
                                         {:success true}))
                                     {:success true})]
    (if-not (:success must-contain-fields-result)
      must-contain-fields-result
      ;; check: doesn't contain keys not defined in 'types-allowed-fields'
      (let [extra-fields-keys (vec (set/difference (set (keys type-map)) (set types-allowed-fields)))]
        (if (> (count extra-fields-keys) 0)
          {:success false
           :fail-point :extra-keys
           :offending-keys extra-fields-keys}
          ;; check individual fields
          ;; description
          (if-not (validate-if-present type-map :description #(util/valid-string? false 1 Integer/MAX_VALUE (:description type-map)))
            {:success false
             :fail-point :description}
            ;; triggers-build
            (if-not (validate-if-present type-map :triggers-build #(boolean? (:triggers-build type-map)))
              {:success false
               :fail-point :triggers-build}
              ;; version-increment
              (let [validate-version-increment-result (validate-version-increment type-map)]
                (if-not (:success validate-version-increment-result)
                  validate-version-increment-result
                  (let [type-map (if (contains? type-map :version-increment)
                                   (assoc type-map :version-increment (:version-increment validate-version-increment-result))
                                   type-map)]
                    ;; direction-of-change
                    (let [validate-direction-of-change-result (validate-direction-of-change type-map)]
                      (if-not (:success validate-direction-of-change-result)
                        validate-direction-of-change-result
                        (let [type-map (if (contains? type-map :direction-of-change)
                                         (assoc type-map :direction-of-change (:direction-of-change validate-direction-of-change-result))
                                         type-map)]
                          ;; check num-scopes
                          (if-not (validate-if-present type-map :num-scopes #(util/valid-coll? false 1 2 (fn [x] (util/valid-integer? false 1 2 x)) (:num-scopes type-map)))
                            {:success false
                             :fail-point :num-scopes}
                            {:success true
                             :type-map type-map}))))))))))))))


(defn validate-type-maps
  "Validates the type-maps `specific-type-map` all at once contained by either 'add' or 'update'.  If valid, returns a
  map with key ':success' to 'true' and key ':type-map' to set to the updated type-maps.  If invalid, returns a map with
  key ':success' to 'false' and key ':reason' set to the reason for the failure.

  On success, updates to the type-map include:
    - :version-increment = converted to keywords
    - :direction-of-change = converted to keywords

  Specifically, the `specific-type-map` is the map at either 'type-override.add' or 'type-override.update'.

  The input `specific-type-map` must have been evaluated for 'add' (keys not in defaults) or 'update' (keys in
  defaults).

  Valid:
    - if `must-contain-all-fields` is 'true', then all fields in 'types-allowed-fields' must be present
    - doesn't contain keys not defined in 'types-allowed-fields'
    - the fields, if set:
    - 'description is a string of length one character to Integer/MAX_VALUE (inclusive)
    - 'triggers-build' is a boolean
    - 'version-increment' is a String whose keyword is contained in 'types-version-increment-allowed-values'
    - 'direction-of-change' is a String whose keyword is contained in 'types-direction-of-change-allowed-values'
    - 'num-scopes' is a vector containing integers '1' or '2'
  "
  [specific-type-map must-contain-all-fields property]
  (let [all-keys (keys specific-type-map)
        validate-type-map-results (map (fn [cur-key]
                                         (let [cur-map (cur-key specific-type-map)]
                                           (assoc (validate-type-map cur-map must-contain-all-fields) :parent-key cur-key)))
                                       all-keys)
        validate-type-map-results-fail (filter #(if (:success %)
                                                  false
                                                  %)
                                               validate-type-map-results)]
    (if (seq validate-type-map-results-fail)
      (let [first-err-map (first validate-type-map-results-fail)]
        (case (:fail-point first-err-map)
          :required-keys (validate-config-fail (str "Property '" property "' missing required keys: " (str/join ", " (mapv name (:offending-keys first-err-map))) "."))
          :extra-keys (validate-config-fail (str "Property '" property "' contained unrecognized keys: " (str/join ", " (mapv name (:offending-keys first-err-map))) "."))
          :description (validate-config-fail (str "Property '" property ".description' must be set as a non-empty string."))
          :triggers-build (validate-config-fail (str "Property '" property ".triggers-build' must be set as a boolean."))
          :version-increment-format (validate-config-fail (str "Property '" property ".version-increment' must be a non-empty string with one of the following values: " (str/join ", " (mapv name types-version-increment-allowed-values))  "."))
          :version-increment-allowed (validate-config-fail (str "Property '" property ".version-increment' must be a non-empty string with one of the following values: " (str/join ", " (mapv name types-version-increment-allowed-values)) "."))
          :direction-of-change-format (validate-config-fail (str "Property '" property ".direction-of-change' must be a non-empty string with one of the following values: " (str/join ", " (mapv name types-direction-of-change-allowed-values)) "."))
          :direction-of-change-allowed (validate-config-fail (str "Property '" property ".direction-of-change' must be a non-empty string with one of the following values: " (str/join ", " (mapv name types-direction-of-change-allowed-values)) "."))
          :num-scopes (validate-config-fail (str "Property '" property ".num-scopes' must be a list of integers with one to two of the following values: 1, 2."))
          (validate-config-fail (str "Property '" property "' encountered an unrecognized error."))))
      {:success true
       :type-map (into {} (map #(do {(:parent-key %) (:type-map %)}) validate-type-map-results))})))


(defn validate-map-of-type-maps
  "Checks that the map `map-of-type-maps` isn't nil, is a map, and isn't an empty map.  Returns a map with the result
  key ':success' true if valid else ':success' to 'false' with reason ':reason'.  A failure result is returned if
  `map-of-type-maps` is nil."
  [map-of-type-maps property]
  (if (nil? map-of-type-maps)
    (validate-config-fail (str "Property '" property "' cannot be nil."))
    (if-not (map? map-of-type-maps)
      (validate-config-fail (str "Property '" property "', if set, must be a non-empty map of maps."))
      (let [keys-in-map-of-type-maps (keys map-of-type-maps)]
        (if-not (> (count keys-in-map-of-type-maps) 0)
          (validate-config-fail (str "Property '" property "', if set, must be a non-empty map of maps."))
          {:success true})))))


(defn validate-config-type-override-add
  "Validates the 'type-override.add' field and returns a map with ':success' set to 'true' with the original 'data'
  else ':success' is set to 'false'.  Updates 'type-override.add', if present, to convert 'version-increment' and
  'direction-of-change' to keywords.

  The 'type-override.add' field is valid if:
    - the property is not set (including if the map is nil)
    - if set, is set to a map (not nil) that is not empty such that for the map's keys:
      - map keys must not be contained in the type defaults 'default-types' (and, implicitly, they are not in 'update'
        or 'remove')
      - at fields, must be set:
        - 'description' is a string of length one character to Integer/MAX_VALUE (inclusive)
        - 'triggers-build' is a boolean
        - 'version-increment' is a String whose keyword is contained in 'types-version-increment-allowed-values'
        - 'direction-of-change' is a String whose keyword is contained in 'types-direction-of-change-allowed-values'
        - 'num-scopes' is a vector containing integers '1' or '2'
  "
  [data]
  (if-not (contains? (get-in data [:config :type-override]) :add)
    (assoc data :success true)
    (let [add-map (get-in data [:config :type-override :add])
          validate-map-of-type-maps-result (validate-map-of-type-maps add-map "type-override.add")]
      (if-not (:success validate-map-of-type-maps-result)
        validate-map-of-type-maps-result
        (let [intersect-keys (vec (set/intersection (set (keys add-map)) (set (keys default-types))))]
          (if (> (count intersect-keys) 0)
            (validate-config-fail (str "Property 'type-override.add' includes types that are defined in the default types: " (str/join ", " (mapv name intersect-keys)) "."))
            (let [validate-specific-type-map-result (validate-type-maps add-map true "type-override.add")]
              (if-not (:success validate-specific-type-map-result)
                (-> data
                    (assoc :success false)
                    (assoc :reason (:reason validate-specific-type-map-result)))
                (-> data
                    (assoc :success true)
                    (assoc-in [:config :type-override :add] (:type-map validate-specific-type-map-result)))))))))))


(defn validate-config-type-override-update
  "Validates the 'type-override.update' field and returns a map with ':success' set to 'true' with the original 'data'
  else ':success' is set to 'false'.  Updates 'type-override.update', if present, to convert 'version-increment' and
  'direction-of-change', if present, to keywords.

  The 'type-override.update' field is valid if:
    - the property is not set (including if the map is nil)
    - if set, is set to a map (not nil) that is not empty such that for the map's keys:
      - map key(s) must be contained in the type defaults 'default-types' (and, implicitly, they are not in 'add')
      - map key(s) must not be contained in the non-editable types 'non-editable-default-types'
      - map key(s) must not be contained in 'type-override.remove' (checked in 'validate-config-type-override-remove')
      - at least one of these fields, must be set:
        - 'description' is a string of length one character to Integer/MAX_VALUE (inclusive)
        - 'triggers-build' is a boolean
        - 'version-increment' is a String whose keyword is contained in 'types-version-increment-allowed-values'
        - 'direction-of-change' is a String whose keyword is contained in 'types-direction-of-change-allowed-values'
        - 'num-scopes' is a vector containing integers '1' or '2'
  "
  [data]
  (if-not (contains? (get-in data [:config :type-override]) :update)
    (assoc data :success true)
    (let [update-map (get-in data [:config :type-override :update])
          validate-map-of-type-maps-result (validate-map-of-type-maps update-map "type-override.update")]
      (if-not (:success validate-map-of-type-maps-result)
        validate-map-of-type-maps-result
        (let [diff-default-keys (vec (set/difference (set (keys update-map)) (set (keys default-types))))]
          (if (> (count diff-default-keys) 0)
            (validate-config-fail (str "Property 'type-override.update' includes types that are not defined in the default types: " (str/join ", " (mapv name diff-default-keys)) "."))
            (let [intersect-non-editable-keys (vec (set/intersection (set (keys update-map)) (set non-editable-default-types)))]
              (if (> (count intersect-non-editable-keys) 0)
                (validate-config-fail (str "Property 'type-override.update' attempts to update non-editable types: " (str/join ", " (mapv name intersect-non-editable-keys)) "."))
                (let [validate-specific-type-map-result (validate-type-maps update-map false "type-override.update")]
                  (if-not (:success validate-specific-type-map-result)
                    (-> data
                        (assoc :success false)
                        (assoc :reason (:reason validate-specific-type-map-result)))
                    (-> data
                        (assoc :success true)
                        (assoc-in [:config :type-override :update] (:type-map validate-specific-type-map-result)))))))))))))


(defn validate-config-type-override-remove
  "Validates the 'type-override.remove' field and returns a map with ':success' set to 'true' with the original `data`
  else ':success' is set to 'false'.  Updates 'type-override.remove', if present, to convert strings to keywords.

  The 'type-override.remove' field contains a list of types from default types in 'default-types' to remove.

  The 'type-override.remove' field is valid if:
    - the property is not set (including if the map is nil)
    - if set, is set to a collection of 1 to Integer/MAX_VALUE elements where elements of the collection
      - are strings
      - length 1 to Integer/MAX_VALUE
      - are not duplicates
      - are keys in the default types (and, implicitly, they are not in 'add')
      - do not duplicate entries in the 'type-override.update' field, if set"
  [data]
  (if-not (contains? (get-in data [:config :type-override]) :remove)
    (assoc data :success true)
    (if-not (util/valid-map-entry? [:config :type-override :remove] false false
                                   (partial util/valid-coll? false 1 Integer/MAX_VALUE
                                            (partial util/valid-string? false 1 Integer/MAX_VALUE))
                                   data)
      (validate-config-fail "Property 'release-branches.remove', if set, must be defined as an array of one or more non-empty strings.")
      (let [remove-as-keywords (mapv keyword (get-in data [:config :type-override :remove]))
            difference-with-default (vec (set/difference (set remove-as-keywords) (set (keys default-types))))]
        (if (> (count difference-with-default) 0)
          (validate-config-fail (str "Property 'release-branches.remove' includes types that are not in the default types: " (str/join ", " (mapv name difference-with-default)) "."))
          (let [data (assoc
                       (assoc-in data [:config :type-override :remove] remove-as-keywords)
                       :success true)]
            (if-not (contains? (get-in data [:config :type-override]) :update)
              data
              (let [intersection-with-update (set/intersection (set remove-as-keywords) (set (keys (get-in data [:config :type-override :update]))))]
                (if (> (count intersection-with-update) 0)
                  (validate-config-fail (str "Property 'release-branches.remove' includes types that are also defined in 'release-branches.update': " (str/join ", " (mapv name intersection-with-update)) "."))
                  data)))))))))


;; todo-next - test
(defn validate-config-type-override
  "
  Valid if:
    - 'type-override' is not defined
    - 'type-override' is defined as a map and at least one of the following is defined and valid (and none invalid):
      - 'type-override.add'
      - 'type-override.update'
      - 'type-override.remove'

  For 'type-override.add':
    - todo

  For 'type-override.update':
    - todo

  For 'type-override.remove':
    - the property is not set (including if the map is nil)
    - if set, is set to a collection of 1 to Integer/MAX_VALUE elements where elements of the collection
      - are strings
      - length 1 to Integer/MAX_VALUE
      - are not duplicates
      - are keys in the default types
      - do not duplicate entries in the 'type-override.update' field, if set
  "
  [data]
  (if (contains? (:config data) :type-override)
    (if (or
          (nil? (get-in data [:config :type-override]))
          (not (map? (get-in data [:config :type-override]))))
      (validate-config-fail "Property 'type-override' must be a map.")
      (if-not (or
                (seq (get-in data [:config :type-override :add]))
                (seq (get-in data [:config :type-override :update]))
                (seq (get-in data [:config :type-override :remove])))
        (validate-config-fail "Property 'type-override' is defined but does not have 'add', 'update', or 'remove' defined.")
        (->> data
             (util/do-on-success validate-config-type-override-add)
             (util/do-on-success validate-config-type-override-update)
             (util/do-on-success validate-config-type-override-remove))))
    (assoc data :success true)))


;(defn validate-config-for-root-project
;  "Validates the root project, returning the data with key 'success' to 'true' if valid other 'false' with key 'reason'
;   with the reason.  Root project must be checked for appropriate structure before checking config with recursion.  The
;   root project is different from subprojects because former structure is a map while latter is a vector."
;  [data]
;  (let [project (get-in data [:config :project])]
;    (if (nil? project)
;      (validate-config-fail "Property 'project' must be defined at the top-level." data)
;      (if (map? project)
;        (assoc data :success true)
;        (validate-config-fail "Property 'project' must be a map." data)))))
;
;
;(defn validate-config-get-depends-on
;  "Returns a vector of two-tuple vectors consisting of each 'depends-on' value and the 'json-path' or an empty vector if
;   no 'depends-on' value exists."
;  [depends-on json-path]
;  (into [] (remove nil? (map
;                         (fn [itm] (if (empty? itm)
;                                     nil
;                                     [itm json-path])) depends-on))))
;
;
;(defn validate-config-project-artifact-common
;  "Validates the project/artifact located at `json-path` in the map `data`, returning the `data` with key 'success' set
;   to 'true' and key 'depends-on' with a vector of vectors pairs of each scope and `json-path`.  The 'depends-on'
;   vector is empty if there are no 'depends-on entries.'  Does NOT validate that 'depends-on' references defined project
;   scopes or does not create cycles.  If invalid, returns 'false' with 'reason' reason.
;
;   The `node-type` may be either ':project' or ':artifact' so that the error message uses the appropriate descriptor."
;  [node-type json-path data]
;  (let [data (if (nil? (:depends-on data))
;               (assoc data :depends-on [])
;               data)
;        node (get-in data json-path)
;        node-descr (if (= :project node-type)
;                     "Project"
;                     "Artifact")]
;    (if (validate-config-param-string node [:name] true false)
;      (let [name (:name node)]
;        (if (validate-config-param-string node [:description] false false)
;          (if (validate-config-param-string node [:scope] true false)
;            (if (validate-config-param-string node [:scope-alias] false false)
;              (if (validate-config-param-array node [:types] true string?)
;                (if (validate-config-param-array node [:depends-on] false string?)
;                  (if (nil? (:project node))
;                    (-> data
;                        (assoc :success true)
;                        (assoc :depends-on (into [] (concat (:depends-on data) (validate-config-get-depends-on (get-in node [:depends-on]) json-path)))))
;                    (validate-config-fail (str node-descr " cannot have property 'project' at property 'name' of '" name "' and path '" json-path "'.") data))
;                  (validate-config-fail (str node-descr " optional property 'depends-on' at property 'name' of '" name "' and path '" json-path "' must be an array of strings.") data))
;                (validate-config-fail (str node-descr " required property 'types' at property 'name' of '" name "' and path '" json-path "' must be an array of strings.") data))
;              (validate-config-fail (str node-descr " optional property 'scope-alias' at property 'name' of '" name "' and path '" json-path "' must be a string.") data))
;            (validate-config-fail (str node-descr " required property 'scope' at property 'name' of '" name "' and path '" json-path "' must be a string.") data))
;          (validate-config-fail (str node-descr " optional property 'description' at property 'name' of '" name "' and path '" json-path "' must be a string.") data)))
;      (validate-config-fail (str node-descr " required property 'name' at path '" json-path "' must be a string.") data))))
;
;
;(defn validate-config-project-specific
;  "Validates the project located at `json-path` in the map `data` for project-specific properties, returning the `data`
;   with key 'success' set to 'true' on success and otherwise 'false' with 'reason' reason.  The 'name' in the target
;   `json-path` path in `data` must be validated.  Does NOT validate the individual artifacts, if any."
;  [json-path data]
;  (let [node (get-in data json-path)
;        name (:name node)]
;    (if (validate-config-param-array node [:projects] false map?)
;      (if (validate-config-param-array node [:artifacts] false map?)
;        (assoc data :success true)
;        (validate-config-fail (str "Project optional property 'artifacts' at property 'name' of '" name "' and path '" json-path "' must be an array of objects.") data))
;      (validate-config-fail (str "Project optional property 'projects' at property 'name' of '" name "' and path '" json-path "' must be an array of objects.") data))))
;
;
;(defn validate-config-artifact-specific
;  "Validates the artifact located at `json-path` in the map `data` for artifact-specific properties, returning the
;   `data` with key 'success' set to 'true' on success and otherwise 'false' with 'reason' reason.  The 'name' in the
;   target `json-path` path in `data` must be validated."
;  [json-path data]
;  (let [node (get-in data json-path)
;        name (:name node)]
;    (if (nil? (:projects node))
;      (if (nil? (:artifacts node))
;        (assoc data :success true)
;        (validate-config-fail (str "Artifact cannot have property 'artifacts' at property 'name' of '" name "' and path '" json-path "'.") data))
;      (validate-config-fail (str "Artifact cannot have property 'projects' at property 'name' of '" name "' and path '" json-path "'.") data))))
;
;
;(defn validate-config-artifacts
;  "Validates the artifacts, if any defined, located at '`json-path` :artifacts' in the map `data` , returning the `data`
;   with key 'success' set to 'true' on success and with 'depends-on' containing a vector of dependent scope paths, if
;   any.  If invalid, returns 'false' with 'reason' reason."
;  [json-path data]
;  (let [json-path-artifacts (conj json-path :artifacts)
;        artifacts (get-in data json-path-artifacts)]
;    (if (empty? artifacts)
;      (assoc data :success true)
;      (let [results (map-indexed (fn [idx _] (validate-config-project-artifact-common :artifact (conj json-path-artifacts idx) (dissoc data :depends-on))) artifacts)
;            depends-on (reduce into [] (remove nil? (map (fn [itm] (if (empty? (:depends-on itm))
;                                                                     nil
;                                                                     (:depends-on itm))) results)))
;            results-err (filter (fn [v] (false? (:success v))) results)]
;        (if (empty? results-err)
;          (let [results-specific (filter (fn [v] (false? (:success v))) (map-indexed (fn [idx _] (validate-config-artifact-specific (conj json-path-artifacts idx) data)) artifacts))]
;            (if (empty? results-specific)
;              (-> data
;                  (assoc :success true)
;                  (assoc :depends-on (into [] (concat (:depends-on data) depends-on))))
;              (first results-specific)))
;          (first results-err))))))
;
;
;(defn validate-config-project-artifact-lookahead
;  "Validates the array of nodes (projects or artifacts) at the `json-path` in `data` and returns a map with key 'true'
;   if valid and 'false' otherwise with key 'reason'.  Error messages include the `node-type`, set with either
;   ':project', ':artifact', or ':both'.  Returns successful if no nodes found."
;  [node-type json-path data]
;  (let [nodes (if (coll? (first json-path))
;                (into [] (apply concat (map (fn [path] (get-in data path)) json-path)))
;                (get-in data json-path))
;        node-descr (if (= :project node-type)
;                     "Project"
;                     (if (= :artifact node-type)
;                       "Artifact"
;                       "Project/Artifact"))]
;    (if (some? nodes)
;      (let [name-resp (col/get-frequency-on-properties-on-array-of-objects nodes [:name])]
;        (if (empty? name-resp)
;          (let [descr-resp (col/get-frequency-on-properties-on-array-of-objects nodes [:description])]
;            (if (empty? descr-resp)
;              (let [scope-resp (col/get-frequency-on-properties-on-array-of-objects nodes [:scope :scope-alias])]
;                (if (empty? scope-resp)
;                  (assoc data :success true)
;                  (validate-config-fail (str node-descr " has duplicate value '" (apply str scope-resp) "' for required property 'scope' / optional property 'scope-alias' at path '" json-path "'.") data)))
;              (validate-config-fail (str node-descr " has duplicate value '" (apply str descr-resp) "' for optional property 'description' at path '" json-path "'.") data)))
;          (validate-config-fail (str node-descr " has duplicate value '" (apply str name-resp) "' for required property 'name' at path '" json-path "'.") data)))
;      (assoc data :success true))))
;
;
;(defn validate-config-projects
;  "Validates the projects in the config at [:config :project] in `data` returning a map result which is the with key
;   'success' to 'true' if valid else set to 'false' with 'reason' set to the reason for the  failure.  Does not validate
;   the top-level project.
;
;   Uses breadth-first traversal because easier to check for name/scope/alias conflict at same level of tree.  Due to
;   JSON structure of the config file, the config is acyclic EXCEPT for 'depends-on' which is validated separately."
;  [data]
;  (loop [queue [[:config :project]]
;         depends-on []]
;    (if (empty? queue)
;      (-> data
;          (assoc :success true)
;          (assoc :depends-on depends-on))
;      (let [json-path (first queue)
;            result (->> (assoc data :success true)
;                        (util/do-on-success validate-config-project-artifact-common :project json-path)
;                        (util/do-on-success validate-config-project-specific json-path)
;                        (util/do-on-success validate-config-artifacts json-path)
;                        (util/do-on-success validate-config-project-artifact-lookahead :artifact (conj json-path :artifacts))
;                        (util/do-on-success validate-config-project-artifact-lookahead :project (conj json-path :projects))
;                        (util/do-on-success validate-config-project-artifact-lookahead :both [(conj json-path :artifacts) (conj json-path :projects)]))]
;        (if (:success result)
;          (if (nil? (get-in data (conj json-path :projects)))
;            (recur (vec (rest queue)) (into [] (concat depends-on (:depends-on result))))
;            (recur (into (vec (rest queue)) (map (fn [itm] (conj json-path :projects itm)) (range (count (get-in data (conj json-path :projects)))))) (into [] (concat depends-on (:depends-on result)))))
;          result)))))
;
;
;(defn update-children-get-next-child-scope-path
;  "If the node defined by `cur-node-json-path` isn't visited (e.g., such that :visited is not set), then updates the
;   current node as visited (e.g., sets :visited to 'true') and adds child nodes (including 'depends-on'), if any, to
;   ':unvisited-children' (less the next child).  Returns a map result with key ':config' containing the updated config
;   and key ':scope-path' containing the full scope path formatted (dot separated) of the next child.
;
;   If the node was visited and has a next unvisited child, then updates the `config` property to remove the next
;   unvisited child and returns the
;
;   If not visited, then updates the current node as visited and adds child nodes (including 'depends-on'), if any.
;   Whether visited or not, returns the next child node in , if any, along with the updated config that reflects the changes
;   mentioned here.  Returns a map with the result with key ':config' for the config and key ':scope-path'.
;
;   Part of the 'depends-on' cycle validation."
;  [cur-node-json-path config]
;  (let [config (if (nil? (:visited (get-in config cur-node-json-path)))
;                 (-> config
;                     (assoc-in (conj cur-node-json-path :visited) true)
;                     (assoc-in (conj cur-node-json-path :unvisited-children) (get-child-nodes-including-depends-on (get-in config cur-node-json-path) config)))
;                 config)]
;    (if (empty? (get-in config (conj cur-node-json-path :unvisited-children)))
;      {:config config
;       :scope-path nil}
;      (let [unvisited-children (get-in config (conj cur-node-json-path :unvisited-children))
;            config (assoc-in config (conj cur-node-json-path :unvisited-children) (rest unvisited-children))]
;        {:config config
;         :scope-path (:full-scope-path-formatted (first unvisited-children))}))))
;
;
;;; todo: can this be combined with the first BFS to avoid a 3rd traversal?
;(defn add-full-paths-to-config
;  "Adds to each project and artifact:  :full-json-path, :full-scope-path, and :full-scope-path-formatted.  Performs
;   a depth-first traversal.  Part of the 'depends-on' cycle validation."
;  [config]
;  (loop [stack [{:json-path [:project]   ;; vector json-type path in the config map
;                 :parent-scope-path []}] ;; vector of parent scopes
;         config config]
;    (if (empty? stack)
;      config
;      (let [node-descr (peek stack)
;            node (get-in config (:json-path node-descr))
;            scope-path (conj (:parent-scope-path node-descr) (get-scope node))
;            child-node-descr {:parent-scope-path scope-path}
;            config (-> config
;                       (assoc-in (conj (:json-path node-descr) :full-json-path) (:json-path node-descr))
;                       (assoc-in (conj (:json-path node-descr) :full-scope-path) scope-path)
;                       (assoc-in (conj (:json-path node-descr) :full-scope-path-formatted) (str/join "." scope-path)))]
;        (recur (into [] (concat (pop stack)
;                                (get-child-nodes node child-node-descr (:json-path node-descr)))) config)))))
;
;
;(defn validate-config-depends-on
;  "Validates 'depends-on' refers to scopes that do not create cycles.  The config in `data` must be valid (particularly
;   that 'depends-on' refers to defined scope paths), other than the possibility of cycles.  Performs two depth-first
;   traversals."
;  [data]
;  (loop [config (add-full-paths-to-config (:config data))
;         recursion-stack [(:full-scope-path-formatted (get-in config [:project]))]] ;; nodes uniquely identified by their full scope path formatted, e.g. proj.alpha.sub
;    (if (empty? recursion-stack)
;      data
;      (let [cur-node-scope-path-formatted (peek recursion-stack)
;            {cur-node-scope-path :scope-path
;             cur-node-json-path :json-path} (find-scope-path cur-node-scope-path-formatted config)
;            {config :config
;             next-child-node-scope-path-formatted :scope-path} (update-children-get-next-child-scope-path cur-node-json-path config)]
;        (if (and
;             (some? next-child-node-scope-path-formatted)
;             (.contains recursion-stack next-child-node-scope-path-formatted))
;          {:success false
;           :reason (str "Cycle detected at traversal path '" recursion-stack "' with scope path '" (:json-path (find-scope-path next-child-node-scope-path-formatted config)) "' for scope '" next-child-node-scope-path-formatted "'.")}
;          (if (nil? next-child-node-scope-path-formatted)
;            (recur config (pop recursion-stack))
;            (recur config (conj recursion-stack next-child-node-scope-path-formatted))))))))


;; todo update docs:
;;  - changes to keywords:
;;     - release-branches
;;     - type-overrides
;;     - scopes, scope-alias, types
;;  - adds full scope path?
(defn validate-config
  "Performs validation of the config file 'config'.  Returns a map result with key ':success' of 'true' if valid and
   'false' otherwise.  If invalid, then returns a key ':reason' with string reason why the validation failed.

   If validation is successful, then a modified 'enhanced' config is returned in ':config' such that: (todo)
     -

   Performs three passes on the config file:  one uses breadth-first traversal which makes it easier to validate name/
   scope/alias at the same level of the tree, and two use depth-first traversal which makes it easier to validate
   cycles.  The two could be combined, but are left separate for ease of implementation and has minimal performance
   impact due to the small sizes of config files.
   
   Ignores properties not used by this tool to allow other systems to use the same project definition config."
  [config]
  (let [data {:config config :success true}
        result (->> data
                    ;; todo: enable these
                    (util/do-on-success validate-config-version)
                    (util/do-on-success validate-config-msg-enforcement)
                    (util/do-on-success validate-config-commit-msg-length)
                    (util/do-on-success validate-config-release-branches)
                    ;; todo add type-override
                    ;; (util/do-on-success validate-config-type-override)
                    ;(util/do-on-success validate-config-for-root-project)   ;; checks that property exists and is a map
                    ;(util/do-on-success validate-config-projects)           ;; performs breadth-first traversal
                    ;(util/do-on-success validate-config-depends-on)
                    )]       ;; performs two depth-first traversals
    result))




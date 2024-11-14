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
  (:require [clojure.string                  :as str]
            [semver-multi.common.collections :as col]
            [semver-multi.common.util        :as util]))


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
           :description "Add code for a future feature (later inidicated as complete with 'feat'). Support branch abstraction."
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


;; todo: validate-scalar then take fn for type?

;(defn validate-config-param-string
;  "Returns boolean 'true' if the value at vector 'key-path' in map 'data' is a string and 'false' otherwise."
;  [data key-path required emptyOk]
;  (let [val (get-in data key-path)]
;    (if (or required val)
;      (if (and (string? val) (or emptyOk (not (= "" val))))
;        true
;        false)
;      true)))


;; todo:
;;  - map (defn validate-col-in-map)
;;    - need key-path
;;  - array (defn validate-col)
;;    - required
;;    - allow-empty
;;    - allow-duplicates
;;    - fn for type of col e.g. vec ?
;;    - fn for type of value e.g. string?


(defn validate-config-param-array
  "Returns boolean 'true' if for all elements in map 'data' at vector 'key-path' the application of 'fn' to those
   elements is 'true' and if 'required' is 'true' or if that location is set; 'false' otherwise."
  [data key-path required fn]
  (if (or required (get-in data key-path))
    (and (vector? (get-in data key-path))
      (> (count (get-in data key-path)) 0)
      (not (.contains (vec (map fn (get-in data key-path))) false)))
    true))


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
  "Validates the 'release-branches' field is set and is a vector of Strings.  Updates ':config' in data by converting
  the strings in 'release-branches' to keywords, and returns the updated map 'data' with key ':success' set to boolean
  'true' if valid or boolean 'false' and ':reason' set to a string message."
  [data]
  (if (validate-config-param-array data [:config :release-branches] true string?)
    (assoc
      (update-in data [:config :release-branches] (partial mapv keyword))
      :success true)
    (validate-config-fail "Property 'release-branches' must be defined as an array of one or more strings.")))


;;; todo - test
;(defn validate-config-type-override-add
;  [data]
;  (if (seq (get-in data [:config :type-override :add]))
;    ;; do stuff
;    (assoc data :success true)))
;
;
;;; todo - test
;(defn validate-config-type-override-update
;  [data]
;  (if (seq (get-in data [:config :type-override :update]))
;    ;; do stuff
;    (assoc data :success true)))
;
;
;;; todo - test
;(defn validate-config-type-override-remove
;  [data]
;  (if (seq (get-in data [:config :type-override :remove]))
;    ;; do stuff
;    (assoc data :success true)))
;
;
;;; todo - test
;(defn validate-config-type-override
;  [data]
;  (if (seq (get-in data [:config :type-override]))
;    (->> data
;       (util/do-on-success validate-config-type-override-add)
;       (util/do-on-success validate-config-type-override-update)
;       (util/do-on-success validate-config-type-override-remove))
;    (assoc data :success true)))
;
;
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




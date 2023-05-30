(ns clojuredocs.template.var
  (:require [clojure.string :as str]
            [clojuredocs.util :as util]
            [clojuredocs.context :refer [*router*]]
            [clojuredocs.template.namespace :as template-ns]
            [reitit.core :as r]))

(defn link
  [{:var/keys [name namespace]}]
  [:a {:href (-> *router*
                 (r/match-by-name
                  :clojuredocs.router/var
                  {:namespace namespace
                   :name      name})
                 (r/match->path))}
   name])

(defn group-vars [vars]
  (->> vars
       (group-by
        (fn [v]
          (let [char (-> v :var/name name first str/lower-case)]
            (if (< (int (first char)) 97)
              "*^%"
              char))))
       (sort-by #(-> % first))
       (map (fn [[c vs]]
              {:heading c
               :vars    vs}))))

(defn $var-group [{:keys [heading vars]}]
  [:div.var-group
   [:h4.heading heading]
   (vec
    (concat
     [:dl.dl-horizontal]
     (->> vars
          (map (fn [{:var/keys [namespace name docstring]
                     :as       var}]
                 (let [html-enc-name
                       (-> name
                           (str/replace #"<" "&lt;")
                           (str/replace #">" "&gt;"))]
                   [:div.dl-row
                    [:dt.name (link var)]
                    (if docstring
                      [:dd.doc docstring]
                      [:dd.no-doc "no doc"])]))))))])


(defn header [{:var/keys [namespace name]}]
  [:div.row.var-header
   [:div.col-sm-8
    [:h1.var-name name]]
   [:div.col-sm-4
    [:div.var-meta
     [:h4 (template-ns/link {:namespace/symbol namespace})]
     #_
     (when added
       [:span "Available since " added])
     #_
     (when-let [su (source-url v)]
       [:span.source-link
        " ("
        [:a {:href su} "source"]
        ") "])]]
   [:div.col-sm-12
    [:section
     #_
     [:ul.arglists
      (if forms
        (map #($argform %) forms)
        (map #($arglist name %) arglists))]]]]
  )

(defn doc-string
  [{doc :var/docstring}]
  [:div.docstring
   (if doc
     [:pre (-> doc
               (str/replace #"\n\s\s" "\n")
               util/html-encode)]
     [:div.null-state "No Doc"])
   (when doc
     [:div.copyright
      "&copy; Rich Hickey. All rights reserved."
      " "
      [:a {:href "http://www.eclipse.org/legal/epl-v10.html"}
       "Eclipse Public License 1.0"]])])

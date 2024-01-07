(ns clojuredocs.router
  (:require [muuntaja.core :as m]
            [reitit.coercion.malli :as coercion-malli]
            [clojuredocs.context :refer [*router*]]
            [clojuredocs.query :as query]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]
            [clojuredocs.template.namespace :as template-namespace]
            [clojuredocs.template.library :as template-library]
            [clojuredocs.template.var :as template-var]
            [clojuredocs.template :as template]
            [datomic.api :as d]
            [reitit.ring :as ring]))

;; TODO: move over all routes to this table
(def table
  [["/n/:namespace"
    {:name       ::namespace
     :parameters {:path {:namespace simple-symbol?}}
     :get        (template/render
                  (fn [{:keys [parameters db]}]
                    (let [{:keys [namespace]} (:path parameters)]
                      (when-let [namespace (query/find-namespace db namespace)]
                        {:content
                         [:div
                          [:div.row
                           [:div.col-sm-2.sidenav
                            [:div
                             {:data-sticky-offset "20"}
                             (template-library/span namespace)]]
                           [:div.col-sm-10
                            (template-namespace/title namespace)
                            [:section.markdown
                             (when-let [doc (:namespace/docstring namespace)]
                               [:pre.doc doc])
                             (template-namespace/markdown namespace)]
                            [:section
                             [:h5 "Vars in " (:namespace/symbol namespace)]
                             (->> (query/find-vars db namespace)
                                  template-var/group-vars
                                  (map template-var/$var-group))]]]]}))))}]
   ["/v/:namespace/:name"
    {:name       ::var
     :parameters {:path {:namespace simple-symbol?
                         :name      simple-symbol?}}
     :get        (template/render
                  (fn [{:keys [parameters db]}]
                    (let [{:keys [namespace name]} (:path parameters)]
                      (when-let [var (query/find-var db namespace name)]
                        {:content [:div
                                   [:div.row
                                    [:div.col-sm-2.sidenav
                                     [:div.desktop-side-nav {:data-sticky-offset "10"}
                                      [:div.var-page-nav]
                                      (template-library/ns-tree nil)]]
                                    [:div.col-sm-10
                                     (template-var/header var)
                                     [:section (template-var/doc-string var)]
                                     [:section
                                      [:div.examples-widget {:id "examples"}]]
                                     [:section
                                      [:div.see-alsos-widget {:id "see-also"}]]
                                     [:section
                                      [:div.notes-widget {:id "notes"}]]]]]}))))}]
   ["/assets/*" (ring/create-resource-handler)]])

(defn datomic-interceptor
  [datomic]
  {:name  ::datomic-interceptor
   :enter (fn [context]
            (-> context
                (assoc-in [:request :datomic] datomic)
                (assoc-in [:request :db] (d/db datomic))))})

(defn make-router
  [{:keys [datomic]}]
  (http/router
   table
   {:data
    {:coercion coercion-malli/coercion
     :muuntaja m/instance
     :interceptors
     [(datomic-interceptor datomic)
      (parameters/parameters-interceptor)
      (muuntaja/format-negotiate-interceptor)
      (muuntaja/format-response-interceptor)
      #_
      (exception/exception-interceptor)
      (muuntaja/format-request-interceptor)
      (coercion/coerce-response-interceptor)
      (coercion/coerce-request-interceptor)
      (multipart/multipart-interceptor)]
     }}))

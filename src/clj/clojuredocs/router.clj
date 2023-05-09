(ns clojuredocs.router
  (:require [muuntaja.core :as m]
            [reitit.coercion.malli :as coercion-malli]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]
            [datomic.api :as d]))

;; TODO: move over all routes to this table
(def table
  [])

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

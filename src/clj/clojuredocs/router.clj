(ns clojuredocs.router
  (:require [muuntaja.core :as m]
            [reitit.coercion.malli :as coercion-malli]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]))

(def table
  [["/" {:name ::awesome :get (constantly {:status 200 :body {:todo true}})}]])

(defn make-router
  [_]
  (http/router
   table
   {:data
    {:coercion coercion-malli/coercion
     :muuntaja m/instance
     :interceptors
     [(parameters/parameters-interceptor)
      (muuntaja/format-negotiate-interceptor)
      (muuntaja/format-response-interceptor)
      (exception/exception-interceptor)
      (muuntaja/format-request-interceptor)
      (coercion/coerce-response-interceptor)
      (coercion/coerce-request-interceptor)
      (multipart/multipart-interceptor)]}}))

(def component
  {:gx/start {:gx/processor
              (fn [{:keys [props]}]
                (make-router props))}})

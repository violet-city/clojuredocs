(ns clojuredocs.router
  (:require [muuntaja.core :as m]
            [reitit.coercion.malli :as malli]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]))

(def trigger-endpoint
  {:name       ::trigger
   :parameters {:body [:map
                       [:operation keyword?]]}
   :post       (fn trigger-handler
                 [req]
                 {:op (-> req :parameters :body :operation)})})

(def swagger-endpoint
  {:get {:no-doc  true
         :swagger {:info                {:title       "Violet Swamp"
                                         :description "Ingesting Government Resources"
                                         :version     "0.0.0"}
                   :securityDefinitions {"auth" {:type :apiKey
                                                 :in   :header
                                                 :name "Example-Api-Key"}}}
         :handler (swagger/create-swagger-handler)}})

(def table
  [["/swagger.json" swagger-endpoint]
   ["/api"
    ["/trigger" trigger-endpoint]]])

(defn assoc-interceptor
  [key prop]
  {:enter #(assoc-in % [:request key] prop)})

(defn job-runner-interceptor
  [job-runner]
  {:leave
   (fn [context]
     (if-let [op (-> context :response :op)]
       (do
         (tap> op)
         (job-runner op)
         (assoc-in context [:response] {:status 200 :body {:job-submitted op}}))
       context))})

(defn make-router
  [{:keys [job-runner]}]
  (http/router
   table
   {:data
    {:coercion reitit.coercion.malli/coercion
     :muuntaja m/instance
     :interceptors
     [swagger/swagger-feature
      (parameters/parameters-interceptor)
      (muuntaja/format-negotiate-interceptor)
      (muuntaja/format-response-interceptor)
      (exception/exception-interceptor)
      (muuntaja/format-request-interceptor)
      (coercion/coerce-response-interceptor)
      (coercion/coerce-request-interceptor)
      (multipart/multipart-interceptor)
      (job-runner-interceptor job-runner)]}}))

(def component
  {:gx/start {:gx/processor
              (fn [{:keys [props]}]
                (make-router props))}})

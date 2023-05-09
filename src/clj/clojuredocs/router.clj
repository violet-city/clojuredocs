(ns clojuredocs.router
  (:require [muuntaja.core :as m]
            [clojuredocs.util :as util]
            [somnium.congomongo :as mon]
            [reitit.coercion.malli :as coercion-malli]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]))


(defn redirect-endpoint
  ([path-or-fn] (redirect-endpoint 301 path-or-fn))
  ([code path-or-fn]
   {:get
    (fn [r]
      {:status  code
       :headers {"Localization" (if (fn? path-or-fn) (path-or-fn r) path-or-fn)}})}))

(defn redirect-to-var []
  (redirect-endpoint
   (fn [r]
     (let [ns   (-> r :parameters :path)
           name (-> r :parameters :name)
           name (->> name
                     util/cd-decode
                     util/cd-encode)]
       (str "/" ns "/" name)))))

(defn old-v-page-redirect []
  (redirect-endpoint
   307
   (fn [r]
     (try
       (let [id (-> r :parameters :id)
             {:keys [ns name] :as legacy-var}
             (mon/fetch-one :legacy-var-redirects :where {:function-id id})]
         (when legacy-var
           (str "/" ns "/" (util/cd-encode name))))
       (catch Exception e nil)))))

(defn ns-redirect
  []
  (redirect-endpoint
   (fn [r]
     (str "/" (-> r :parameters :name)))))

#_
(def old-url-redirects
  [["/clojure_core/:ns/:name" (redirect-to-var)]
   ["/clojure_core/:version/:ns/:name" (redirect-to-var)]
   ["/quickref/*" (redirect-endpoint "/quickref")]
   ["/clojure_core" (redirect-endpoint "/core-library")]
   ["/clojure_core/:ns" (redirect-endpoint (str "/" ns))]
   ["/v/:id" (old-v-page-redirect)]
   ["/examples_style_guide" (redirect-endpoint "/examples-styleguide")]
   ["/Clojure%20Core/:ns" [ns] {:status 301 :headers {"Location" (str "/" ns)}}]
   ["/Clojure%20Core/:ns/" [ns] {:status 301 :headers {"Location" (str "/" ns)}}]
   ["/Clojure%20Core" [] {:status 301 :headers {"Location" "/core-library"}}]
   ["/clojure_core/:ns/" [ns] {:status 301 :headers {"Location" (str "/" ns)}}]
   ["/clojure_core" [] {:status 301 :headers {"Location" "/core-library"}}]])


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

(ns clojuredocs.service
  (:require [reitit.http :as r]
            [muuntaja.interceptor]
            [reitit.interceptor.sieppari :as sieppari]
            [prone.middleware :as prone]))

(defn make
  [{:keys [router compojure]}]
  (-> (r/ring-handler
       router
       compojure ;; TODO: this is a hacky way to migrate over to reitit, slowely.
       {:executor     sieppari/executor
        :interceptors [(muuntaja.interceptor/format-interceptor)]})
      (prone/wrap-exceptions)))

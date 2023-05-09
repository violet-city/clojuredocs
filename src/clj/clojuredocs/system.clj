(ns clojuredocs.system
  (:require [clojuredocs.css :as css]
            [clojuredocs.datomic :as datomic]
            [clojuredocs.entry :as entry]
            [clojuredocs.pages :as pages]
            [clojuredocs.service :as service]
            [clojuredocs.router :as router]
            [clojuredocs.api.server :as api.server]
            [clojuredocs.env :as env]
            [clojure.edn :as edn]
            [garden.core :as garden]
            [ring.adapter.jetty :as jetty]
            [somnium.congomongo :as mon]
            [taoensso.timbre :as log]
            [prone.middleware :as prone]))

(defn component
  ([name start]
   (component name start nil))
  ([name start stop]
   (cond-> {:gx/start
            {:gx/processor
             (fn [node]
               (log/info ::starting name)
               (start node))}}
           stop (assoc-in [:gx/stop :gx/processor]
                          (fn [node]
                            (log/info ::stopping name)
                            (stop node))))))

(def routes
  (component
    ::routes
    (fn [{:keys [props]}]
      (let [pages-routes (pages/make-routes)
            api-routes   (api.server/make-routes)]
        (entry/make-routes {:datomic     (:datomic props)
                            :api-routes  api-routes
                            :page-routes pages-routes})))))

(def router
  (component
    ::router
    (fn [{:keys [props]}]
      (router/make-router props))))

(def service
  (component
    ::service
    (fn [{:keys [props]}]
      (service/make props))))

(def app
  (component
    ::app
    (fn [{:keys [props]}]
      (log/info props)
      {:port      (env/int :port 8080)
       :entry     (:service props)
       :mongo-url (env/str :mongo-url)})))

(def server
  (component
   ::server
   (fn [node]
     (let [{:keys [props]}      node
           {:keys [port entry]} (:app props)]
       (jetty/run-jetty
        (prone/wrap-exceptions
         (fn [r]
           (let [resp (entry r)]
             (if (:status resp)
               resp
               (do
                 (tap> resp)
                 (assoc resp :status 200))))))
          {:port port :join? false})))
    (fn [node]
      (.stop (:value node)))))

(def datomic
  (component
    ::datomic
    (fn [{:keys [props]}]
      (datomic/start! (:uri props) (:schema props)))
    (fn [{:keys [value]}]
      (datomic/stop! value))))

(def mongo
  (component
    ::mongo
    (fn [{:keys [props]}]
      (let [{:keys [username password]} props]
        (doto (mon/make-connection "clojuredocs" :username username :password password)
          (mon/set-connection!))))
    (fn [{:keys [value]}]
      (mon/close-connection value))))

(defn setup-fixtures
  [_]
  ;; NOTE: this whole block is inlined from https://github.com/zk/clojuredocs/blob/f6d6faf14e4b79beca34a92a06dd69546002eeea/src/clj/clojuredocs/main.clj#L51
  ;;       I'm not sure if this is bug or not.
  (comment
    (mon/add-index! :examples [:account-source])
    (mon/add-index! :examples [:account.account-source])
    (mon/add-index! :examples [:account.login])
    (mon/add-index! :examples [:author.account-source])
    (mon/add-index! :examples [:author.login])
    (mon/add-index! :examples [:deleted-at])
    (mon/add-index! :examples [:editor.account-source])
    (mon/add-index! :examples [:editor.login])
    (mon/add-index! :examples [:editors.account-source])
    (mon/add-index! :examples [:editors.login])
    (mon/add-index! :examples [:email])
    (mon/add-index! :examples [:from-var.library-url])
    (mon/add-index! :examples [:from-var.name])
    (mon/add-index! :examples [:from-var.ns])
    (mon/add-index! :examples [:function-id])
    (mon/add-index! :examples [:login])
    (mon/add-index! :examples [:migraion-key])
    (mon/add-index! :examples [:name])
    (mon/add-index! :examples [:namespaces])
    (mon/add-index! :examples [:to-var.library-url])
    (mon/add-index! :examples [:to-var.name])
    (mon/add-index! :examples [:to-var.ns])
    (mon/add-index! :examples [:var])
    (mon/add-index! :examples [:var.library-url])
    (mon/add-index! :examples [:var.name])
    (mon/add-index! :examples [:var.ns])))

(def fixtures
  (component
    ::fixtures
    setup-fixtures))

(def css
  (component
    ::css
    (fn [_]
      (garden/css
        {:output-to     "resources/public/css/app.css"
         :pretty-print? false
         :vendors       ["webkit" "moz" "ms"]
         :auto-prefix   #{:justify-content
                          :align-items
                          :flex-direction
                          :flex-wrap
                          :align-self
                          :transition
                          :transform
                          :box-shadow}}
        css/app))))

(def secrets
  (component
    ::secrets
    (fn [_]
      (let [credentials (edn/read-string (slurp ".secrets/credentials.edn"))]
        ;; TODO: obviously don't store secrets here.
        (merge
         credentials
         {:db/username      "postgres"
          :db/password      "example"})))))

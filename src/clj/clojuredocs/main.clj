(ns clojuredocs.main
  (:gen-class)
  (:require [k16.gx.beta.system :as gx.system]
            [k16.gx.beta.core :as gx]
            [clojure.edn :as edn]
            [taoensso.timbre :as logger]))

#_#_#_#_#_#_#_#_(defn compile-css []
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
                    css/app))

        (defn start-http-server [entry-point opts]
          (jetty/run-jetty
            (fn [r]
              (let [resp (entry-point r)]
                (if (:status resp)
                  resp
                  (assoc resp :status 200))))
            opts))

        (defn create-app []
          {:port      (env/int :port 8080)
           :entry     #'entry/routes
           :mongo-url (env/str :mongo-url)})

        (defn report-and-exit-on-missing-env-vars! []
          (when-not (empty? config/missing-env-vars)
            (println)
            (println "!!! Missing Env Vars:")
            (doseq [{:keys [key doc type]} config/missing-env-vars]
              (println "!!! " key (str "[" type "]:") doc))
            (println "!!! Exiting...")
            (println)
            (System/exit -1)))

        (defn add-indexes-to-coll! [why ks]
          (doseq [k ks]
            (mon/add-index! :examples [k])))

        (defn add-all-indexes! []
          (add-indexes-to-coll!
            :examples [:var :deleted-at
                       :author.login :author.account-source
                       :editors.login :editors.account-source])

          (add-indexes-to-coll! :namespaces [:name])

          (add-indexes-to-coll!
            :see-alsos [:from-var.name :from-var.ns :from-var.library-url
                        :to-var.ns :to-var.name :to-var.library-url
                        :account.login :account.account-source])

          (add-indexes-to-coll! :libraries [:namespaces])

          (add-indexes-to-coll!
            :notes [:var.ns :var.name :var.library-url
                    :account.login :account.account-source])

          (add-indexes-to-coll!
            :legacy-var-redirects [:function-id
                                   :editor.login :editor.account-source])

          (add-indexes-to-coll! :users [:login :account-source])

          (add-indexes-to-coll! :migrate-users [:email :migraion-key]))

        (defn start-app []
          (compile-css)
          (let [{:keys [mongo-url port entry]} (create-app)
                mongo-conn (mon/make-connection mongo-url :username "admin" :password "example")]
            (report-and-exit-on-missing-env-vars!)
            (mon/set-connection! mongo-conn)
            (add-all-indexes!)
            (let [stop-server (start-http-server entry
                                                 {:port port :join? false})]
              (println (format "Server running on port %d" port))
              (fn []
                (mon/close-connection mongo-conn)
                (.stop stop-server)))))

        (defn stop-app [f]
          (when f (f)))

(defn load-system!
  []
  (gx.system/register!
    ::system
    {:context gx/default-context
     :graph   (edn/read-string (slurp "config.edn"))}))

(defn start!
  []
  (gx.system/signal! ::system :gx/start))

(defn stop!
  []
  (gx.system/signal! ::system :gx/stop))

(defn failures
  []
  (gx.system/failures-humanized ::system))

(defn reload!
  []
  (stop!)
  (load-system!)
  (start!))

(defn -main
  [& _]
  (load-system!)
  (doto (Runtime/getRuntime)
    (.addShutdownHook
      (Thread. #(stop!))))
  (start!)
  (when-let [failures (seq (failures))]
    (doseq [failure failures]
      (logger/error failure))
    (System/exit 1))
  @(promise))

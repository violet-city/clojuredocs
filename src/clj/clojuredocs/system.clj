(ns clojuredocs.system)

(defn component
  ([start]
   (component start nil))
  ([start stop]
   {:gx/start {:gx/processor start}
    :gx/stop  {:gx/processor stop}}))

(def server
  (component
   (fn start-server
     [{:keys [props]}])))

(def service
  (component
   (fn start-service
     [{:keys [props]}])))

(def router
  (component
   (fn start-router
     [{:keys [props]}])))

(def xtdb
  (component
   (fn start-xtdb
     [{:keys [props]}])))

(def secrets
  (component
   (fn start-secrets
     [{:keys [props]}])))

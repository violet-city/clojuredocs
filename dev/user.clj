(ns user
  (:require [k16.gx.beta.system :as gx.system]
            [k16.gx.beta.core :as gx]
            [cljfmt.main]
            [clojure.edn :as edn]))

(defn load-system!
  []
  (gx.system/register!
   ::system
   {:context gx/default-context
    :graph   (-> (edn/read-string (slurp "config.edn"))
                 (assoc :reveal {:gx/component 'user.reveal/component}))}))

(defn start!
  []
  @(gx.system/signal! ::system :gx/start))

(defn stop!
  []
  @(gx.system/signal! ::system :gx/stop))

(defn failures
  []
  (gx.system/failures-humanized ::system))

(defn reload!
  []
  (stop!)
  (load-system!)
  (start!))

(defmacro using
  [component]
  `(~component (gx.system/values ::system)))

(def xtdb #(using :xtdb))
(def server #(using :server))
(def service #(using :service))
(def router #(using :router))
(def cron #(using :cron))

(ns user
  (:require [clojure.edn :as edn]
            [k16.gx.beta.core :as gx]
            [k16.gx.beta.system :as gx.system]
            [clojuredocs.query :as query]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as tac]
            [clojure.core]
            [datomic.api :as d]))

(log/merge-config!
 {:min-level [["clojuredocs.*" :trace]
              ["datomic.*" :warn]]
  :ns-filter #{"clojuredocs.*"
               "datomic.*"}
  :appenders {:println {:enabled? false}
              :spit    (tac/spit-appender {:fname "logs/clojuredocs.log"})}})

(defn load-config!
  []
  (edn/read-string (slurp "config.edn")))

(defn load-system!
  []
  (gx.system/register!
    ::system
    {:context gx/default-context
     :graph   (-> (load-config!)
                  (assoc :reveal {:gx/component 'user.system/reveal})
                  (assoc :shadow {:gx/component 'user.system/shadow}))}))

(def app-selector
  (->> (load-config!)
       (keys)
       (into [])))

(def dev-selector [:reveal :shadow])

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

(def app #(using :app))
(def css #(using :css))
(def fixtures #(using :fixtures))
(def mongo #(using :mongo))
(def secrets #(using :secrets))
(def server #(using :server))
(def datomic #(using :datomic))
(def routes #(using :routes))
(def db #(d/db (datomic)))

(comment

  (tap> (query/top-contribs (db)))
  (tap> (query/recently-updated (db)))

  (d/q
   '{:find [?is]
     :where [[?u :user/source "github"]
             [?u :user/id+source ?is]]}
   (db)
   )

  (d/pull
   (db)
   [:user/id+source]
   [:user/id+source
    ["ZehCnaS34" "github"]])

  :keep)

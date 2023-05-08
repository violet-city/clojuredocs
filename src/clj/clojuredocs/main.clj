(ns clojuredocs.main
  (:gen-class)
  (:require [clojure.edn :as edn]
            [k16.gx.beta.core :as gx]
            [k16.gx.beta.system :as gx.system]
            [taoensso.timbre :as logger]))

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

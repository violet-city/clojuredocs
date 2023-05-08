(ns user
  (:require [clojuredocs.main :as clojuredocs]))


(defn start! []
  (clojuredocs/start-app))


(defn stop! []
  (clojuredocs/stop-app))

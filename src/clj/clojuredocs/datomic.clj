(ns clojuredocs.datomic
  (:require [datomic.api :as d]))

(defn migrate
  [conn schema]
  (when (seq schema)
    @(d/transact conn schema)))

(defn start!
  [uri schema]
  (d/create-database uri)
  (doto (d/connect uri)
    (migrate schema)))

(defn stop! [conn]
  (d/release conn))

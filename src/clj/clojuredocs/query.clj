(ns clojuredocs.query
  (:require [datomic.api :as d]))

;; users

(defn get-user
  [db email]
  (d/pull db '[*] [:user/email email]))

(defn examples-created-by
  [db email]
  (d/q '{:find  [?e]
         :in    [$ ?u]
         :where [[?e :example/author ?u]]}
       db
       [:user/email email]))

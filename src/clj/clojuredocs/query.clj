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

(defn top-contribs
  [db]
  (let [query  '{:find  [?email ?e]
                 :where [[?e :example/author ?a]
                         [?a :user/email ?email]]}
        scores (reduce (fn [score [email]]
                         (update score email (fnil + 0) 4))
                       {}
                       (d/q query db))]
    (->> scores
         (sort-by second >)
         (take 50)
         (map (fn [[email score]]
                {:user/email email
                 :score      score})))))

(defn recently-updated
  [db]
  (let [query  '{:find  [?e ?time]
                 :where [[?e :example/body _ ?tx]
                         [?t :db/txInstant ?time]]}]
    (->> (d/q query db)
         (sort-by second)
         (take 10)
         (map (fn [[e]]
                (d/pull db '[:example/body {:example/author [:user/email]}] e)))
         (reverse))))

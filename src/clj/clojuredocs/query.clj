(ns clojuredocs.query
  (:require [datomic.api :as d]))

(defn find-namespace
  [db name]
  (d/pull db
          [:namespace/symbol
           :namespace/docstring]
          [:namespace/symbol name]))

(defn find-var
  [db namespace name]
  (d/pull db
          [:var/docstring :var/namespace :var/name]
          [:var/ns+name [namespace name]]))

(defn find-vars
  [db {:namespace/keys [symbol]}]
  (->> (d/q '{:find  [(pull ?v [:var/name
                                :var/namespace
                                :var/docstring])]
              :in    [$ ?ns]
              :where [[?v :var/namespace ?ns]]}
            db
            symbol)
       (map first)))

;; users

(defn get-user
  [db id]
  (d/pull db '[*] [:user/id id]))

(defn examples-created-by
  [db id]
  (d/q '{:find  [?e]
         :in    [$ ?u]
         :where [[?e :example/author ?u]]}
       db
       [:user/id id]))

(defn top-contribs
  [db]
  (let [query  '{:find  [?id ?e]
                 :where [[?e :example/author ?a]
                         [?a :user/id ?id]]}
        scores (reduce (fn [score [id]]
                         (update score id (fnil + 0) 4))
                       {}
                       (d/q query db))]
    (->> scores
         (sort-by second >)
         (take 50)
         (map (fn [[id score]]
                {:user/id id
                 :score   score})))))

(defn recently-updated
  [db]
  (let [query  '{:find  [?e ?time]
                 :where [[?e :example/body _ ?tx]
                         [?t :db/txInstant ?time]]}]
    (->> (d/q query db)
         (sort-by second)
         (take 10)
         (map (fn [[e]]
                (d/pull db '[:example/body {:example/author [:user/id]}] e)))
         (reverse))))


(def examples-for-var-q
  '{:find  [?e]
    :in    [$ ?ns ?name]
    :where [[?e :example/for ?v]
            [?n :namespace/symbol ?ns]
            [?v :var/ns ?n]
            [?v :var/name ?name]
            ]})

(defn examples-for-var [db {:keys [ns name]}]
  (let [result (d/q examples-for-var-q db ns name)]
    (sequence result)))

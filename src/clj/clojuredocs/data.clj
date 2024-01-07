(ns clojuredocs.data
  (:require [somnium.congomongo :as mon]
            [datomic.api :as d]
            [clojuredocs.context :refer [*db*]]))

;; Examples

(def find-examples-for-q
  '{:find  [(pull ?e [*])
            (pull ?v [*])]
    :keys  [:example :var]
    :in    [$ ?n ?name]
    :where [[?v :var/name ?name]
            [?v :var/namespace ?n]
            [?e :example/for ?v]]})


(defn find-examples-for [{:keys [ns name library-url]}]
  (println name ns)
  (println (type name) (type ns) *db*)
  (->> (d/q find-examples-for-q *db* ns name)
       (map first)))

(defn find-var [{:keys [ns name]}]
  (println name ns)
  (println (type name) (type ns) *db*)
  (->> (d/q '{:find  [(pull ?v [*])]
              :in    [$ ?n ?name]
              :where [[?v :var/name ?name]
                      [?v :var/namespace ?n]]}
            *db* ns name)
       (map first)
       (first)))

;; Notes

(def find-notes-for-q
  '{:find  [(pull? ?n [*])]
    :keys  [:note]
    :in    [$ ?ns ?name ?library-url]
    :where [[?n :note/var.name ?name]
            [?n :note/var.ns ?ns]
            [?n :note/var.library-url ?library-url]]})

(defn find-notes-for [{:keys [ns name library-url]}]
  (mon/fetch :notes
             :where {:var.ns          ns
                     :var.name        name
                     :var.library-url library-url}
             :sort {:created-at 1}))


;; See Alsos

(defn find-see-alsos-for [{:keys [ns name library-url]}]
  (mon/fetch :see-alsos
             :where {:from-var.name        name
                     :from-var.ns          ns
                     :from-var.library-url library-url}))

(def find-see-alsos-for-q
  '{:find  [(pull? ?s [*])]
    :keys  [:see-also]
    :in    [$ ?ns ?name ?library-url]
    :where [[?s :see-also/var.name ?name]
            [?s :see-also/var.ns ?ns]
            [?s :see-also/var.library-url ?library-url]]})

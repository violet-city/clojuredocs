(ns clojuredocs.data
  (:require [somnium.congomongo :as mon]))

;; Examples

(def find-examples-for-q
  '{:find  [(pull? ?e [*])]
    :keys  [:example]
    :in    [$ ?ns ?name ?library-url]
    :where [[?e :example/var.name ?name]
            [?e :example/var.ns ?ns]
            [?e :example/var.library-url ?library-url]]})

(defn find-examples-for [{:keys [ns name library-url]}]
  (mon/fetch :examples
             :where {:var.name        name
                     :var.ns          ns
                     :var.library-url library-url
                     :deleted-at      nil}
             :sort {:created-at 1}))

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

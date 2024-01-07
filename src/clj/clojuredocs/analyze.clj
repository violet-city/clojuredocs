(ns clojuredocs.analyze
  (:require [malli.core]
            [clojure.core.logic.datomic]
            [clojure-lsp.api :as api]
            [cljs.analyzer]
            [ns-tracker.core]
            [clojure.walk :as walk]
            [clojure.java.io :as io]))


(defn crawl-ns
  [ns]
  (tree-seq simple-symbol?
            (fn [ns]
              (for [[_ ns] (ns-aliases ns)]
                (.name ns)))
            ns))

(def publics->vars
  (comp
   (map second)
   (map meta)
   (map (fn [{:keys [ns name doc]}]
          (merge
           (when doc
             {:var/docstring doc})
           {:var/name      name
            :var/namespace (.name ns)})))
   (distinct)))

(def ns->entities
  (comp
   (map (fn [ns]
          {:namespace/symbol    ns
           :namespace/docstring (:doc (meta ns))}))))

(defn find-vars
  [ns]
  (->> ns
       (crawl-ns)
       (mapcat #(sequence publics->vars (ns-publics %)))))

(defn find-nss
  [ns]
  (->> (crawl-ns ns)
       (map (fn [ns]
              (let [ns (find-ns ns)]
                (merge
                 (when-let [doc (:doc (meta ns))]
                   {:namespace/docstring doc})
                 {:namespace/symbol (.name ns)}))))))

(->> (find-vars 'malli.core)
     (tap>))

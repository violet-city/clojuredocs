(ns clojuredocs.analyze
  (:require [malli.core]
            [clojure.core.logic.datomic]
            [clojure-lsp.api :as api]
            [cljs.analyzer]
            [ns-tracker.core]
            [clojure.walk :as walk]
            [clojure.java.io :as io]))

(def thing
  (api/analyze-project-only!
   {:project-root (io/file ".")}))



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
          {:var/docstring doc
           :var/name      name
           :var/ns        [:namespace/symbol ns]}))))

(def ns->entities
  (comp
   (map (fn [ns]
          {:namespace/symbol ns}))))

(defn find-vars
  [ns]
  (->> ns
       (crawl-ns)
       (mapcat #(sequence publics->vars (ns-publics %)))))

(->> 'malli.core
     (crawl-ns)
     (mapcat #(sequence publics->vars (ns-publics %)))
     (tap>))

(->> 'malli.core
     (crawl-ns)
     (sequence ns->entities)
     (tap>))

(->> 'cljs.analyzer
     (crawl-ns)
     (sequence ns->entities)
     (tap>))

(crawl-ns 'clojure.core)
(crawl-ns 'malli.core)
(crawl-ns 'cljs.analyzer)

(tap>
 (binding [*ns* (find-ns 'clojure.core)]
   (ns-publics *ns*)))

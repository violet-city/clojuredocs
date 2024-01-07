(ns clojuredocs.template.namespace
  (:require [clojure.string :as str]
            [clojuredocs.util :as util]
            [clojuredocs.context :refer [*router*]]
            [reitit.core :as r]))

(defn link
  [{:namespace/keys [symbol]}]
  [:a {:href (-> *router*
                 (r/match-by-name
                  :clojuredocs.router/namespace
                  {:namespace symbol})
                 (r/match->path))}
   symbol])

(defn markdown [ns]
  #_(common/memo-markdown-file (str "src/md/namespaces/" ns-str ".md"))
  )


(defn title
  [{:namespace/keys [symbol]}]
  [:h1 (name symbol)])

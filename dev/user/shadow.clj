(ns user.shadow
  (:require [shadow.cljs.devtools.api :as shadow.api]
            [shadow.cljs.devtools.server :as shadow.server]))

(defonce running? (atom false))

(defn start!
  [_]
  (when-not @running?
    (shadow.server/start!)
    (shadow.api/watch :clojuredocs)))

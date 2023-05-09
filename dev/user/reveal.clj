(ns user.reveal
  (:require [vlaaad.reveal :as r]
            [taoensso.timbre :as log]))

(defn start-reveal
  [_]
  (let [window (r/ui)]
    (add-tap window)
    {:log          #(window %)
     :close-window #(window)}))

(defn stop-reveal
  [node]
  (try
    ((:close-window (:value node)))
    (catch Throwable t
      (tap> t)
      (tap> node))))

(defn splash-reveal
  [node]
  (log/info ::suspend)
  (tap> [:splash])
  (:value node))

(defn clear-reveal
  [node]
  (tap> [:clear])
  (:value node))

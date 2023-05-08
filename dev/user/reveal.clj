(ns user.reveal
  (:require [vlaaad.reveal :as r]))

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

(def component
  {:gx/start {:gx/processor start-reveal}
   :gx/stop  {:gx/processor stop-reveal}})

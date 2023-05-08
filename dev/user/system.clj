(ns user.system
  (:require [user.reveal :as reveal]
            [user.shadow :as shadow]))
(def reveal
  {:gx/start {:gx/processor reveal/start-reveal}
   :gx/stop  {:gx/processor reveal/stop-reveal}})

(def shadow
  {:gx/start {:gx/processor shadow/start!}})

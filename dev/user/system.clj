(ns user.system
  (:require [user.reveal :as reveal]
            [user.shadow :as shadow]))
(def reveal
  {:gx/start   {:gx/processor reveal/start-reveal}
   :gx/resume  {:gx/processor reveal/splash-reveal}
   :gx/suspend {:gx/processor reveal/clear-reveal}
   :gx/stop    {:gx/processor reveal/stop-reveal}})

(def shadow
  {:gx/start {:gx/processor shadow/start!}})

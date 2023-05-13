(ns clojuredocs.library
  (:require [malli.core :as m]))

(def Library
  [:map
   [::uri uri?]
   [::version string?]
   [::source uri?]])


(def valid? (m/validator Library))


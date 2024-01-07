(ns clojuredocs.pages.gh-auth
  (:import (java.net URI))
  (:require [clojuredocs.config :as config]
            [clojuredocs.github :as gh]
            [ring.util.response :refer (redirect)]
            [lambdaisland.uri :as uri :refer [uri]]
            [datomic.api :as d]
            [somnium.congomongo :as mon]))


(defn gh-user->user [{:keys [avatar_url login]}]
  {:avatar-url avatar_url
   :account-source "github"
   :login login})

(defn callback-handler [path]
  (fn [{:keys [params datomic db]}]
    (try
      (let [token   (:access_token (gh/exchange-code config/gh-creds (:code params)))
            gh-user (gh/user token)
            user    (gh-user->user gh-user)
            user'   {:user/id     (:login user)
                     :user/source (:account-source user)
                     :user/avatar (URI. (:avatar-url user))}]
        (if-not (d/pull db
                        [:user/id+source]
                        [:user/id+source [(:user/id user') (:user/source user')]])
          @(d/transact datomic [user'])
          (tap> "welcome back"))
        (-> (redirect (if (empty? path) "/" path))
            (assoc :session (doto {:user      user
                                   :real-user user'}
                              (tap>)))))
      (catch Exception e
        (prn e)
        (tap> e)
        (-> (redirect "/gh-auth")
            (assoc :session nil))))))

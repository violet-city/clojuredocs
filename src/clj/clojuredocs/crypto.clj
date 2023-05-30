(ns clojuredocs.crypto
  (:require
   [clojuredocs.util :as util]
   [clojure.java.io :as jio]))

(defn md5-path [path]
  (try
    (-> path jio/resource slurp util/md5)
    (catch java.io.FileNotFoundException e
      nil)))

(ns clojuredocs.xtdb
  (:import (java.time Duration))
  (:require [clojure.java.io :as io]
            [xtdb.api :as xt]))

(defn start!
  [{:keys [host user password]}]
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (xt/start-node
     {:xtdb/tx-log         {:xtdb/module         'xtdb.jdbc/->tx-log
                            :connection-pool     {:dialect   {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                                  :pool-opts {}
                                                  :db-spec   {:dbname   "clojuredocs_tx_log"
                                                              :dbtype   "postgres"
                                                              :host     host
                                                              :user     user
                                                              :password password}}
                            :poll-sleep-duration (Duration/ofSeconds 1)}
      :xtdb/document-store {:xtdb/module     'xtdb.jdbc/->document-store
                            :connection-pool {:dialect   {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                              :pool-opts {}
                                              :db-spec   {:dbname   "clojuredocs_document_store"
                                                          :dbtype   "postgres"
                                                          :host     host
                                                          :user     user
                                                          :password password}}}
      :xtdb/index-store    (kv-store "data/xtdb/index-store")})))

(defn stop!
  [node]
  (.close node))

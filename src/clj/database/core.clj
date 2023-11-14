(ns database.core
  (:require [xtdb.api :as xt]
            [mount.core :as mount]))

(def connection-pool
  {:xtdb.jdbc/connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.sqlite/->dialect}
                               :db-spec {:dbname "test"}}
   :xtdb/tx-log {:xtdb/module 'xtdb.jdbc/->tx-log
                 :connection-pool :xtdb.jdbc/connection-pool}
   :xtdb/document-store {:xtdb/module 'xtdb.jdbc/->document-store
                         :connection-pool :xtdb.jdbc/connection-pool}})

(mount/defstate node
  :start (xt/start-node connection-pool)
  :stop (.close node))

(defn ensure-id [{:keys [:xt/id] :as m}]
  (if-not id
    (assoc m :xt/id (java.util.UUID/randomUUID))
    m))

(ensure-id {:xt/id 1234})

(defn write [entity]
  (let [with-id (ensure-id entity)]
    (xt/submit-tx node [[::xt/put with-id]])))

(write {:data "Hi"})

#_(defn query []
    (crux/q
     (crux/db node)
     '{:find [(pull ?e [*])]
       :where [[?e :crux.db/id _]] }))
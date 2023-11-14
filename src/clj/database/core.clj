(ns database.core
  (:require [xtdb.api :as xt]
            [mount.core :as mount]))

(mount/defstate node
  :start (xt/start-node {})
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
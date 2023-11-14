(ns database.core
  (:require [crux.api :as crux]
            [mount.core :as mount]))

(mount/defstate node
  :start (crux/start-node {}))

(defn ensure-id [{:keys [:crux.db/id] :as m}]
  (if-not id
    (assoc m :crux.db/id (java.util.UUID/randomUUID))
    m))

(ensure-id {:crux.db/id 1234})

(defn write [entity]
  (let [with-id (ensure-id entity)]
    (crux/submit-tx node [[:crux.tx/put with-id]])))

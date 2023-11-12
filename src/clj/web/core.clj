(ns web.core
  (:require [reitit.ring :as ring]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]))

(def app
  (ring/ring-handler
   (ring/router
    [["/"]]
    {:data {:muuntaja m/instance}})))

(mount/defstate server
  :start (http/start-server #'app {:port 4080})
  :stop (.close server))

(defn -main [& _]
  (mount/start))

(ns web.core
  (:require [reitit.ring :as ring]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [clojure.java.io :as io]))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(def app
  (ring/ring-handler
   (ring/router
    [["/js/*" (ring/create-resource-handler {:root "public/js"})]
     ["/css/*" (ring/create-resource-handler {:root "public/css"})]
     ["/" {:get index-handler}]]
    {:data {:muuntaja m/instance}})))

(mount/defstate server
  :start (http/start-server #'app {:port 4080})
  :stop (.close server))

(defn -main [& _]
  (mount/start))

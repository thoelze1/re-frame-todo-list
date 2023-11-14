(ns web.core
  (:require [reitit.ring :as ring]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [clojure.java.io :as io]
            [database.core :as db]))

(def with-muuntaja (atom nil))
(def without-muuntaja (atom nil))
(def zero-muuntaja (atom nil))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(defn create-handler [{:keys [body-params]}]
  (let [tx (db/write body-params)]
    ;;(reset! zero-muuntaja req)
    (clojure.pprint/pprint tx)
    ;;(clojure.pprint/pprint (slurp (:body req)))
    {:body body-params}))

(def app
  (ring/ring-handler
   (ring/router
    [["/foo" {:post create-handler
              :middleware [:content]}]
     ["/js/*" (ring/create-resource-handler {:root "public/js"})]
     ["/css/*" (ring/create-resource-handler {:root "public/css"})]
     ["/" {:get index-handler}]]
    {::middleware/registry {:content muuntaja/format-middleware}
     :data {:muuntaja m/instance}})))

(mount/defstate server
  :start (http/start-server #'app {:port 4080})
  :stop (.close server))

(defn -main [& _]
  (mount/start))

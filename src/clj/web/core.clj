(ns web.core
  (:require [reitit.ring :as ring]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.keyword-params :as keyword-params]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [clojure.java.io :as io]
            [database.core :as db]
            [web.socket :as socket]))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(defn create-handler [{:keys [body-params]}]
  (let [tx (db/write body-params)]
    ;;(reset! zero-muuntaja req)
    (clojure.pprint/pprint tx)
    ;;(clojure.pprint/pprint (slurp (:body req)))
    {:body body-params}))

(def keyword-params-middleware
  {:name ::keyword-params
   :compile (fn [_data _route-opts]
              keyword-params/wrap-keyword-params)})

(def app
  (ring/ring-handler
   (ring/router
    [["/chsk" {:get #(socket/ring-ajax-get-or-ws-handshake %)
               :post #(socket/ring-ajax-post %)
               :middleware [:params :keyword-params]}]
     ["/sleep" {:post create-handler
                :middleware [:content]}]
     ["/foo" {:post create-handler
              :middleware [:content]}]
     ["/js/*" (ring/create-resource-handler {:root "public/js"})]
     ["/css/*" (ring/create-resource-handler {:root "public/css"})]
     ["/" {:get index-handler}]]
    {::middleware/registry {:content muuntaja/format-middleware
                            :params parameters/parameters-middleware
                            :keyword-params keyword-params-middleware}
     :data {:muuntaja m/instance}})))

(mount/defstate server
  :start (http/start-server #'app {:port 4080})
  :stop (.close server))

(defn -main [& _]
  (mount/start))

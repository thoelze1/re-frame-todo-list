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
            [web.socket :as socket])
  (:import (java.time OffsetDateTime LocalTime LocalDate Instant)))

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

(defn get-midnight
  [odt]
  (-> odt
      (.withHour 0)
      (.withMinute 0)
      (.withNano 0)))

(defn get-next-midnight
  [odt]
  (-> (get-midnight odt)
      (.plusDays 1)))

#_(.plusDays (OffsetDateTime/of (LocalDate/now) (LocalTime/MIDNIGHT) (.getOffset (OffsetDateTime/now))) 1)

(defn interval->date-map [odt1 odt2]
  (loop [f odt1
         m {}]
    (let [midnight (get-next-midnight f)
          data {(get-midnight f) (- (min (.toEpochSecond midnight)
                                         (.toEpochSecond odt2))
                                    (.toEpochSecond f))}]
      (if (.isAfter odt2 midnight)
        (recur midnight (merge m data))
        (merge m data)))))

(defn sleep-history->sleep-data
  [sleep-history]
  (let [timezone (.getOffset (OffsetDateTime/now))]
    (reduce (fn [out {:keys [start stop]}]
              (let [from (OffsetDateTime/ofInstant (Instant/parse start) timezone)
                    to (OffsetDateTime/ofInstant (Instant/parse stop) timezone)]
                (merge-with + out (interval->date-map from to))))
            {}
            sleep-history)))

(defn sleep-data-handler [_]
  {:body (->> '{:find [(pull ?e [:start :stop])]
                :where [[?e :type "sleep"]]}
              (db/query)
              (mapv first) ;; why...
              (sleep-history->sleep-data))})

(def app
  (ring/ring-handler
   (ring/router
    [["/chsk" {:get #(socket/ring-ajax-get-or-ws-handshake %)
               :post #(socket/ring-ajax-post %)
               :middleware [:params :keyword-params]}]
     ["/sleep" {:get sleep-data-handler
                :post create-handler
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

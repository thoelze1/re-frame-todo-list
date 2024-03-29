(ns re-frame-todo-list.socket
  (:require [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [re-frame-todo-list.handlers :as handlers]))

(def router_ (atom nil))

(def ch-chsk (atom nil))
(def chsk-send! (atom nil))
(def chsk-state (atom nil))

(def config {:type     :auto
             :packer   (sente-transit/get-transit-packer) ;:edn
             :protocol :http
             :host     "localhost"
             :port     4080})

(defn state-watcher [_key _atom _old-state new-state]
  (.warn js/console "New state" new-state))

(defn create-client! []
  (let [{:keys [ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk" nil config)]
    (reset! ch-chsk ch-recv)
    (reset! chsk-send! send-fn)
    (add-watch state :state-watcher state-watcher)))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! @ch-chsk handlers/event-msg-handler)))

(defn start! []
  (println "starting websocket client")
  (create-client!)
  (start-router!))

(defn send-data [data]
  (@chsk-send! [:some/client-message data]))
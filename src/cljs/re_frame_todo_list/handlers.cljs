(ns re-frame-todo-list.handlers
  (:require [re-frame.core :as rf]
            [lambdaisland.glogi :as log]))

(defmulti -event-msg-handler :id)

(defn event-msg-handler
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default
  [{:keys [event]}]
  (log/info :unhandled-event event))

(defmethod -event-msg-handler :chsk/recv
  [{:keys [?data]}]
  (let [[event-type data] ?data]
    (rf/dispatch [:app/set-data data])
    (log/info :push-event data)))

(defmethod -event-msg-handler :some/broadcast
  [{:keys [?data]}]
  (let [[event-type data] ?data]
    (rf/dispatch [:get-sleep-chart-data])
    (log/info :push-event data)))
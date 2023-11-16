(ns web.handlers
  (:require [taoensso.timbre :as timbre]))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (timbre/debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:unmatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :chsk/uidport-open
  [{:as ev-msg :keys [ring-req]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (if uid
      (timbre/infof "User connected: user-id `%s`" uid)
      (timbre/infof "User connected: no user-id (user didn't have login session)"))))

(defmethod -event-msg-handler :chsk/uidport-close
  [{:as ev-msg :keys [ring-req]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (if uid
      (timbre/infof "User disconnected: user-id `%s`" uid)
      (timbre/infof "User disconnected: no user-id (user didn't have login session)"))))
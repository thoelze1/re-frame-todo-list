(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   ))

(re-frame.core/reg-event-db
  :add-item
  (fn [db [_ item-str]]
    (update db :items #(conj % item-str))))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (into (subvec coll 0 pos) (subvec coll (inc pos))))

(re-frame.core/reg-event-db
  :delete-item
  (fn [db [_ item-idx]]
    (update db :items #(vec-remove % item-idx))))

(re-frame.core/reg-event-db
  :new-item
  (fn [db [_ item-str]]
    (assoc db :new-item item-str)))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

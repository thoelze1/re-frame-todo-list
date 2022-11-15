(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   ))

(re-frame.core/reg-event-db
  :add-item
  (fn [db [_ item-str]]
    (doall
      (update db :items #(conj % item-str)))))

(re-frame.core/reg-event-db
  :new-item
  (fn [db [_ item-str]]
    (assoc db :new-item item-str)))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

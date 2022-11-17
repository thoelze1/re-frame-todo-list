(ns re-frame-todo-list.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::items
 (fn [db]
   (:items db)))

(re-frame/reg-sub
 ::items-order
 (fn [db]
   (:items-order db)))

(re-frame/reg-sub
 ::selected-item
 (fn [db]
   (:selected-item db)))

(re-frame/reg-sub
 ::drag-prev
 (fn [db]
   (:drag-prev db)))

(re-frame/reg-sub
 ::new-item
 (fn [db]
   (:new-item db)))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

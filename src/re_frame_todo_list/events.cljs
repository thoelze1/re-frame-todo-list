(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [reagent.core :as reagent]
   [goog.events :as events]
   )
  (:import [goog.events EventType]))

(enable-console-print!)

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

;; O(1) append
(re-frame.core/reg-event-db
 :add-item
 (let [id (reagent/atom 21)]
   (fn [db [_ item-str]]
     (do (swap! id inc)
         (let [idx (count (:items db))]
           (update db :items #(conj % {:val item-str
                                       :id @id})))))))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (into (subvec coll 0 pos) (subvec coll (inc pos))))

(defn vec-add
  "add elem in coll"
  [coll pos val]
  (into (conj (subvec coll 0 pos) val) (subvec coll pos)))

(defn vec-reorder
  "move item in coll"
  [coll from to]
  (-> coll
      (vec-remove from)
      (vec-add to (get coll from))))

;; O(n) deletion
(re-frame.core/reg-event-db
 :delete-item
 (fn [db [_ idx]]
   (update db :items vec-remove idx)))

(re-frame.core/reg-event-db
  :edit-new-item
  (fn [db [_ new-str]]
    (assoc db :new-item new-str)))

(re-frame.core/reg-event-db
  :edit-item
  (fn [db [_ item-idx new-str]]
    (assoc-in db [:items item-idx :val] new-str)))

(re-frame.core/reg-event-db
 :reorder
 (fn [db [_ idx1 idx2]]
   (update db :items vec-reorder idx1 idx2)))

(re-frame.core/reg-event-db
 :lift
 (fn [db [_ id]]
   (assoc db :selected-item id)))

(re-frame.core/reg-event-db
 :drop
 (fn [db [_]]
   (assoc db :selected-item nil)))

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
                     (if (and (map? v1) (map? v2))
                       (merge-with deep-merge v1 v2)
                       v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))
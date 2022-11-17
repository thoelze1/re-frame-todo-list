(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [reagent.core :as reagent]
   [goog.events :as events]
   )
  (:import [goog.events EventType]))

(def initial 90)
(def multiple 100)

;; a data structure that can be indexed by two different types of values: an id
;; and an index. simple implementation as:
;; map from index to id
;; map from id to (index,val)
;; if given an id, that's a constant time lookup AND deletion
;; if given an index, that's a constant time lookup AND deletion

(defn idx->height
  [idx]
  (+ initial (* multiple idx)))

;; O(1) append
(re-frame.core/reg-event-db
 :add-item
 (let [id (reagent/atom 21)]
   (fn [db [_ item-str]]
     (do (swap! id inc)
         (let [idx (count (:items db))]
           (-> db
               (update :items-order conj @id)
               (assoc-in [:items @id] {:val item-str
                                       :idx idx
                                       :height (idx->height idx)})))))))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (into (subvec coll 0 pos) (subvec coll (inc pos))))

;; O(n) deletion
(re-frame.core/reg-event-db
 :delete-item
 (fn [db [_ id]]
   (let [idx (get-in db [:items id :idx])]
     (reduce (fn [m i]
               (update-in m [:items (get-in db [:items-order i]) :idx] dec))
             (-> db (update :items dissoc id)                 
                 (update :items-order vec-remove idx))
             (range (inc idx) (count (:items db)))))))

(re-frame.core/reg-event-db
  :new-item
  (fn [db [_ item-str]]
    (assoc db :new-item item-str)))

(re-frame.core/reg-event-fx
 :lift
 (fn [cofx [_ id e]]
   (let [top (-> e .-target .getBoundingClientRect .-top)
         offset (- (.-clientY e) top)]
     {:db (assoc (:db cofx) :selected-item id)
      :listen-drag [id offset]})))

(re-frame.core/reg-fx
 :listen-drag
 (fn [[id offset]]
   (let [f (fn [e] (re-frame.core/dispatch [:drag e offset id]))]
     (events/listen js/window EventType.MOUSEMOVE f)
     (events/listen js/window EventType.MOUSEUP
                    #(do
                       (re-frame/dispatch [:drop id])
                       (events/unlisten js/window EventType.MOUSEMOVE f))))))

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
                     (if (and (map? v1) (map? v2))
                       (merge-with deep-merge v1 v2)
                       v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))

;; drag-prev is redundant; use selected instead
;; use merge to simplify!
(re-frame.core/reg-event-db
 :drag
 (fn [db [_ evt offset id]]
   (let [y (- (.-clientY evt) offset)
         idx (get-in db [:items id :idx])
         swapped-items (fn [other-idx op]
                         (let [other-id (get-in db [:items-order other-idx])
                               other-height (get-in db [:items other-id :height])]
                           {:items {id {:idx other-idx}
                                    other-id {:height (op other-height multiple)
                                              :idx idx}}
                            :items-order (-> (:items-order db)
                                             (assoc idx other-id)
                                             (assoc other-idx id))}))]
     (let [prev-idx (dec idx)
           next-idx (inc idx)
           prev-id (get-in db [:items-order prev-idx])
           next-id (get-in db [:items-order next-idx])]
       (if (and (< (/ (- y initial) multiple) prev-idx)
                (>= prev-idx 0))
         (assoc-in (deep-merge db (swapped-items prev-idx +)) [:items id :height] y)
         (if (and (> (/ (- y initial) multiple) next-idx)
                  (< next-idx (count (:items db))))
           (assoc-in (deep-merge db (swapped-items next-idx -)) [:items id :height] y)
           (assoc-in db [:items id :height] y)))))))

;; is passing id redundant? we have this information in :selected-item (for use
;; by subscriptions, but also usable here)
(re-frame.core/reg-event-db
 :drop
 (fn [db [_ id]]
   ;; also set idx here
   (-> db
       (assoc-in [:items id :height] (idx->height (get-in db [:items id :idx])))
       (dissoc :selected-item))))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

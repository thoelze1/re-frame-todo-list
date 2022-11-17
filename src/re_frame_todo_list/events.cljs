(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [reagent.core :as reagent]
   [goog.events :as events]
   )
  (:import [goog.events EventType]))

(enable-console-print!)

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
           (update db :items #(conj % {:val item-str
                                       :id @id
                                       :height (idx->height idx)})))))))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (into (subvec coll 0 pos) (subvec coll (inc pos))))

;; O(n) deletion
(re-frame.core/reg-event-db
 :delete-item
 (fn [db [_ idx]]
   (update db :items vec-remove idx)))

(re-frame.core/reg-event-db
  :edit-new-item
  (fn [db [_ new-str]]
    (assoc db :new-item new-str)))

(re-frame.core/reg-event-fx
 :lift
 (fn [cofx [_ idx e]]
   (do
     (println "lift")
     (let [top (-> e .-target .getBoundingClientRect .-top)
           offset (- (.-clientY e) top)]
       {:db (assoc (:db cofx) :selected-item idx)
        :listen-drag offset}))))

(re-frame.core/reg-fx
 :listen-drag
 (fn [offset]
   (let [drag (fn [e] (re-frame.core/dispatch [:drag e offset]))
         drop (fn [] (re-frame/dispatch [:drop]))]
     (events/listen js/window EventType.MOUSEMOVE drag)
     (events/listen js/window EventType.MOUSEUP drop)
     (events/listen js/window EventType.MOUSEUP
                    #(do 
                       (events/unlisten js/window EventType.MOUSEUP drop)
                       (events/unlisten js/window EventType.MOUSEMOVE drag))))))

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
                     (if (and (map? v1) (map? v2))
                       (merge-with deep-merge v1 v2)
                       v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))

(comment 
  (re-frame.core/reg-event-db
   :animate
   ;; check if current item is equal to selected item; this is the only case
   ;; when we should stop animation
   (fn [db [_ idx]]
     (assoc-in db [:items idx] (assoc next-item :height (idx->height idx))))))

;; name of :selected-item is misleading; should be called :selected-idx
(re-frame.core/reg-event-db
 :displace
 (fn [db [_ that-idx]]
   (let [that-item (get-in db [:items that-idx])
         this-idx (:selected-item db)
         this-item (get-in db [:items this-idx])]
     (-> db
         ;; next we're going to adjust the following line to include animation
         ;; I wonder if we should use add-item (with an index) to do the swap?
         (assoc-in [:items this-idx] (assoc that-item :height (idx->height this-idx)))
         (assoc-in [:items that-idx] (assoc this-item))
         (assoc :selected-item that-idx)))))

(re-frame.core/reg-event-fx
 :drag
 (fn [cofx [_ evt offset]]
   (let [db (:db cofx)
         y (- (.-clientY evt) offset)
         idx (:selected-item db)]
     {:db (assoc-in db [:items idx :height] y)
      :fx (let [prev-idx (dec idx)]
            (if (and (< (/ (- y initial) multiple) prev-idx)
                     (>= prev-idx 0))
              [[:dispatch [:displace prev-idx]]]
              (let [next-idx (inc idx)]
                (if (and (> (/ (- y initial) multiple) next-idx)
                         (< next-idx (count (:items db))))
                  [[:dispatch [:displace next-idx]]]
                  []))))})))

;; note: updating the height value below is currently redundant, since the view
;; renders the selected item differently than all others; therefore dissoc'ing
;; the selected item is sufficient to have the dragged item snap into place
(re-frame.core/reg-event-db
 :drop
 (fn [db [_]]
   (let [idx (:selected-item db)]
     (do
       (println idx)
       (-> db
           (assoc-in [:items idx :height] (idx->height idx))
           (dissoc :selected-item))))))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

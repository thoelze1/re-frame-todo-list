(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [goog.events :as events]
   )
  (:import [goog.events EventType]))

(def initial 90)
(def multiple 50)

(defn idx->height
  [idx]
  (+ initial (* multiple idx)))

(re-frame.core/reg-event-db
  :add-item
  (fn [db [_ item-str]]
    (update db :items #(conj % {:val item-str
                                :height (idx->height (count (:items db)))}))))

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

(re-frame.core/reg-event-fx
 :drag
 (fn [cofx [_ idx e z]]
   (let [top (-> e .-target .getBoundingClientRect .-top)
         default top
         offset (- (.-clientY e) top)]
     ; not sure how args to custom fxs work
     {:db (-> (:db cofx)
              (assoc-in [:items idx :z] z)
              (assoc :drag-prev (- idx 1)))
      :listen-drag [idx offset (fn [] ())]})))

(re-frame.core/reg-event-db
 :do-drag
 (fn [db [_ evt offset idx]]
   (let [y (- (.-clientY evt) offset)
         prev (:drag-prev db)]
     (if (and (< (/ (- y initial) multiple) prev)
              (>= prev 0))
       (-> db
           (assoc-in [:items idx :height] y)
           (update-in [:items prev :height] (fn [x] (+ x multiple)))
           (update :drag-prev dec))
       (if (and (> (/ (- y initial) multiple) (+ prev 2))
                (< (+ 2 prev) (count (:items db))))
         (-> db
             (assoc-in [:items idx :height] y)
             (update-in [:items (+ prev 2) :height] (fn [x] (- x multiple)))
             (update :drag-prev inc))
         (assoc-in db [:items idx :height] y))))))

(re-frame.core/reg-event-db
 :stop-drag
 (fn [db [_ idx]]
   ; also set idx here
   (assoc-in db [:items idx :height] (idx->height (+ 1 (:drag-prev db))))))

(re-frame.core/reg-fx
 :listen-drag
 (fn [[idx offset]]
   (let [f (fn [e] (re-frame.core/dispatch [:do-drag e offset idx]))]
     (events/listen js/window EventType.MOUSEMOVE f)
     (events/listen js/window EventType.MOUSEUP
                    #(do
                       (re-frame/dispatch [:stop-drag idx])
                       (events/unlisten js/window EventType.MOUSEMOVE f))))))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

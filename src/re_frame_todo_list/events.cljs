(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [goog.events :as events]
   )
  (:import [goog.events EventType]))

(re-frame.core/reg-event-db
  :add-item
  (fn [db [_ item-str]]
    (update db :items #(conj % {:val item-str :height 0}))))

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
 (fn [cofx [_ idx e]]
   (let [;top (-> e .-target .getBoundingClientRect .-top)
         ;offset (- (.-clientY e) top)
         offset (.-clientY e)]
     ; not sure how args to custom fxs work
     {:listen-drag [idx offset]})))

(re-frame.core/reg-event-db
 :do-drag
 (fn [db [_ evt offset idx]]
   (let [y (- offset (.-clientY evt))]
     (assoc-in db [:items idx :height] y))))

(re-frame.core/reg-fx
 :listen-drag
 (fn [[idx offset]]
   (let [f (fn [e] (re-frame.core/dispatch [:do-drag e offset idx]))]
     (events/listen js/window EventType.MOUSEMOVE f)
     (events/listen js/window EventType.MOUSEUP
                    #(events/unlisten js/window EventType.MOUSEMOVE f)))))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

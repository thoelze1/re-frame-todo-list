(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [reagent.core :as reagent]
   [goog.events :as events]
   )
  (:import [goog.events EventType]))

(enable-console-print!)

(def initial 0)
(def multiple 60)

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
 (fn [cofx [_ idx e id on-lift on-drop]]
   (do
     (println "lift")
     (on-lift)
     (let [top (-> e .-target .getBoundingClientRect .-top)
           offset (- (.-clientY e) top)]
       {:db (assoc (:db cofx) :selected-item idx)
        :listen-drag {:offset 50;offset
                      :id id
                      :on-drop on-drop}}))))

(re-frame.core/reg-fx
 :listen-drag
 (fn [m]
   (let [offset (:offset m)
         id (:id m)
         on-drop (:on-drop m)
         drag (fn [e] (re-frame.core/dispatch [:drag e offset]))
         drop (fn [] (re-frame/dispatch [:drop id on-drop]))]
     
     ;(-> (js/document.getElementById 8)  (.addEventListener "animationend" (fn [] (println "hi"))))
     ;(goog.dom.setProperties (js/document.getElementById 8) (clj->js {:onmousedown #(println "hi")}))
     
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

(re-frame.core/reg-event-db
 :snap
 ;; the index is in the wrong place and needs to be animated towards the correct
 ;; place, which is determined by its index.
 ;;
 ;; if this function is non-atomic, e.g. if it slowly animates the target index
 ;; toward its correct position, we will need to check if this index is ever
 ;; equal to the selected item in which case the selected item has been dragged
 ;; back into this item's place and so we need to stop animation so that this
 ;; index can be animated toward a new location
 (fn [db [_ idx]]
   (assoc-in db [:items idx :height] (idx->height idx))))

;; name of :selected-item is misleading; should be called :selected-idx
(re-frame.core/reg-event-fx
 :displace
 (fn [cofx [_ that-idx]]
   (let [db (:db cofx)
         that-item (get-in db [:items that-idx])
         this-idx (:selected-item db)
         this-item (get-in db [:items this-idx])]
     {:db (-> db
              (assoc-in [:items this-idx] that-item)
              (assoc-in [:items that-idx] this-item)
              (assoc :selected-item that-idx))
      :fx [[:dispatch [:snap this-idx]]]})))

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

;; https://stackoverflow.com/questions/41656640/clojure-javascript-interop-multiple-parameters

;; note: updating the height value below is currently redundant, since the view
;; renders the selected item differently than all others; therefore dissoc'ing
;; the selected item is sufficient to have the dragged item snap into place
(re-frame.core/reg-event-db
 :drop
 (fn [db [_ id on-drop]]
   (let [idx (:selected-item db)
         elem (-> js/document (.getElementById 8)) ]
     (do
       (goog.dom.setProperties (-> elem
                                   (.animate 
                                    (clj->js [{:transform "rotate(0) scale(1)"}
                                              {:transform "rotate(360deg) scale(0)"}])
                                    (clj->js {:duration 2000
                                              :iterations 1})))
                               (clj->js {:onfinish #(println "hi")}))
       
       (println "elem: " elem)
       (println id)
       ;;(on-drop)
       (-> db
           (assoc-in [:items idx :height] (idx->height idx))
           (dissoc :selected-item))))))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(ns re-frame-todo-list.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.db :as db]
   [reagent.core :as reagent]
   [goog.events :as events]
   [ajax.core :as ajax]
   [re-frame-todo-list.socket :as socket])
  (:import [goog.events EventType]))

(enable-console-print!)

(re-frame/reg-event-fx
 :app/send
 (fn [_ [_ data]]
   (socket/send-data data)
   ;; TODO
   {}))

(re-frame/reg-event-fx
 :app/connect
 (fn [_ _]
   (socket/start!)
   {}))

(re-frame.core/reg-event-db
 :handler
 (fn [db [_ res]]
   (.log js/console (str res))))

(re-frame.core/reg-event-db
 :error-handler
 (fn [db [_ {:keys [status status-text]}]]
   (.log js/console (str "something bad happened: " status " " status-text))))

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
                                       :id @id
                                       :done {}})))))))

(re-frame.core/reg-event-fx
 :helper
 (fn [cofx [_]]
   {:http-xhrio {:method          :post
                 :uri             "http://localhost:4080/foo"
                 :params          {:data "using http-fx"}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:handler]
                 :on-failure      [:error-handler]}}))

(re-frame.core/reg-event-db
 :add-expense
 (fn [db [_ m]]
   (update-in db [:expenses :expense-list] conj m)))

;; data manipulation could go here, but printing date e.g. wants to be done in
;; views
(re-frame.core/reg-event-db
 :update-sleep-chart-data
 (fn [db [_ res]]
   (assoc db :sleep-data res)))

(re-frame.core/reg-event-fx
 :get-sleep-chart-data
 (fn [_ [_ from to]]
   {:http-xhrio {:method :get
                 :uri "http://localhost:4080/sleep"
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:update-sleep-chart-data]
                 :on-failure      [:error-handler]}}))

;; from and to are Date objects
(re-frame.core/reg-event-fx
 :add-sleep-interval
 (fn [_ [_ from to]]
   {:http-xhrio {:method          :post
                 :uri             "http://localhost:4080/sleep"
                 :params          {:type :sleep
                                   :start from ;; could use .getTime to send
                                   :stop to}   ;;   unix-time instead...
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:handler]
                 :on-failure      [:error-handler]}}))

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
  :mark-item-date-yes
  (fn [db [_ item-idx date]]
    (assoc-in db [:items item-idx :done date] true)))

(re-frame.core/reg-event-db
  :mark-item-date-no
  (fn [db [_ item-idx date]]
    (assoc-in db [:items item-idx :done date] false)))

(re-frame.core/reg-event-db
  :unmark-item-date
  (fn [db [_ item-idx date]]
    (assoc-in db [:items item-idx :done date] nil)))

(re-frame.core/reg-event-db
  :complete-item
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
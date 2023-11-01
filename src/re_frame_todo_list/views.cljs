(ns re-frame-todo-list.views
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.subs :as subs]
   [re-com.core   :refer [at h-box v-box box gap line label title slider checkbox input-text horizontal-bar-tabs vertical-bar-tabs p]]
   [re-com.slider :refer [slider-parts-desc slider-args-desc]]
   [reagent.core  :as    reagent]
   ["react-beautiful-dnd" :as react-beautiful-dnd]
   ["recharts" :as recharts]
   ["react-datepicker$default" :as DatePicker]
   [re-frame-todo-list.macros :as macros]
   ))

;; the todo items should comprise a priority level; therefore this priority
;; level (which I'm using as `height` or `idx`) should be stored in the db the
;; priority level is a data structure abstraction; the "height" is simply a
;; visual abstraction. therefore the height related information should live in
;; the view context, and perhaps passed as necessary to the events context

;; https://stackoverflow.com/questions/33446913/reagent-react-clojurescript-warning-every-element-in-a-seq-should-have-a-unique
;; https://stackoverflow.com/questions/24239144/js-console-log-in-clojurescript

(def drag-drop-context (reagent/adapt-react-class react-beautiful-dnd/DragDropContext))
(def droppable (reagent/adapt-react-class react-beautiful-dnd/Droppable))
(def draggable (reagent/adapt-react-class react-beautiful-dnd/Draggable))

(def line-chart (reagent/adapt-react-class recharts/LineChart))
(def cartesian-grid (reagent/adapt-react-class recharts/CartesianGrid))
(def xaxis (reagent/adapt-react-class recharts/XAxis))
(def yaxis (reagent/adapt-react-class recharts/YAxis))
(def rechart-line (reagent/adapt-react-class recharts/Line))

(def datepicker (reagent/adapt-react-class DatePicker))

(defn item-view
  [index item]
  [:div.row.rounded.bg-cyan-300.p-1
   [:div.col-6.p-0.bg-cyan-300
    (let [id (str "super-unique-string" (:id item))]
      ;; the second child of the flex container needs padding. possibly relevant:
      ;; https://stackoverflow.com/questions/23717953/padding-bottom-top-in-flexbox-layout
      [:div.flex.items-center
       [:i.fa-solid.fa-ellipsis-vertical.px-2]
       [:input.w-100.rounded.pl-2.pt-1.pb-1.pr-1.bg-cyan-200
        {:type "text"
         :id id
         :value (:val item)
         ;; perhaps this :on-change should filter newline
         :on-change #(re-frame/dispatch [:edit-item index (-> % .-target .-value)])
         :on-key-press #(if (= 13 (.-charCode %))
                          (.blur (.getElementById js/document id)))}]])]
   (doall
    (map
     (fn [i]
       (let [date (keyword (.toDateString (js/Date. (+ (js/Date.now) (* 1000 60 60 24 i)))))]
         [:div.col.align-self-center
          {:style {:cursor :pointer}
           :key (str i "specisl" index)
           :on-click #(do (.preventDefault %)
                          (re-frame/dispatch [:mark-item-date-yes index date]))}
          [:div.text-center
           (let [status (get-in item [:done date])]
             (cond
               (= nil status) [:i.fa.fa-question-circle.text-cyan-200]
               (= true status) [:i.fa.fa-check.text-emerald-200]
               (= false status) [:i.fa.fa-x.text-red-200]))]]))
     (range 0 -5 -1)))
   [:div.col.align-self-center.text-center
    [:i.fa.fa-trash.text-center.text-red-900
     {:style {:cursor :pointer}
      :on-click #(re-frame/dispatch [:delete-item index])}]]])

(defn dates
  []
  [:div.row
   [:div.col-6]
   (doall
    (map
     (fn [i]
       (let [date (keyword (.toDateString (js/Date. (+ (js/Date.now) (* 1000 60 60 24 i)))))]
         [:div.col.align-self-center
          [:div.text-center
           date]]))
     (range 0 -5 -1)))
   [:div.col]])

(defn chart-view
  []
  [line-chart
   {:height 300 :width 500 :data (clj->js '({:name "test"
                                             :amount 10}
                                            {:name "test2"
                                             :amount 5}
                                            {:name "test3"
                                             :amount 6}
                                            {:name "test4"
                                             :amount 8}))}
   ;; why oh why do the props to recharts need to be camelcase? the props for
   ;; rbd are in kebab case...
   [cartesian-grid
    {:strokeDasharray "3 3"}]
   [xaxis {:dataKey "name"}]
   [yaxis]
   [rechart-line {:type "monotone" :dataKey (clj->js :amount) }]])

(defn items-header
  []
  [:p.text-center.text-white.text-3xl.font-mono.m-2 "Your Habits"])


;; maybe height should be a component-local reagent atom
(defn items-view
  []
  (let [items (re-frame/subscribe [::subs/items])
        selected-item (re-frame/subscribe [::subs/selected-item])
        moving? (reagent/atom nil)
        panel-dnd-id "todo-list-dnd"]
    [:div
     [items-header]
     [dates]
     [drag-drop-context
      {:onDragStart  (fn [result]
                       (let [index (get-in (js->clj result) ["source" "index"])]
                         (re-frame/dispatch [:lift index])))
       :onDragUpdate (fn [result] (println result))
       :onDragEnd    (fn [result]
                       (let [r (js->clj result)]
                         (if (get r "destination")
                           (let [src (get-in r ["source" "index"])
                                 dst (get-in r ["destination" "index"])]
                             (do
                               (re-frame/dispatch [:reorder src dst])
                               (re-frame/dispatch [:drop]))))))}
      [droppable
       {:droppable-id panel-dnd-id :type "thing"}
       (let [items @items]
         (fn [provided snapshot]
           (reagent/as-element
            [:div (merge {:ref   (.-innerRef provided)
                          :class (when (.-isDraggingOver snapshot) :drag-over)}
                         (js->clj (.-droppableProps provided)))
             (doall
              (map-indexed
               (fn [index item]
                 ^{:key (:id item)}
                 [:div.container
                  [:div.row
                   ;;{:style {:margin 10}}
                   [draggable {:draggable-id (str (:id item)), :index index}
                    (fn [provided snapshot]
                      (reagent/as-element
                       ;; https://github.com/atlassian/react-beautiful-dnd/issues/1855
                       [:div.mb-2 (merge {:ref (.-innerRef provided)}
                                         (js->clj (.-draggableProps provided))
                                         (js->clj (.-dragHandleProps provided)))
                        [item-view index item]]))]]])
               items))
             (.-placeholder provided)])))]]]))

(defn item-input
  []
  (let [new-item (re-frame/subscribe [::subs/new-item])
        gettext  (fn [e] (-> e .-target .-value))
        touch    (fn [e] (re-frame/dispatch [:edit-new-item (gettext e)]))
        add-item #(do
                    (re-frame/dispatch [:add-item @new-item])
                    (re-frame/dispatch [:edit-new-item ""]))]
    [:div
     [:input
      {:type "text"
       :value @new-item
       ;; perhaps this :on-change should filter newline
       :on-change touch
       :on-key-press (fn [e] (if (= 13 (.-charCode e)) (add-item) ))}]
     [:input
      {:type "button"
       :value "Add item"
       :on-click add-item}]]))


(defn sleep-view
  []
  [:p "hi"]
  [datepicker])

;; https://github.com/atlassian/react-beautiful-dnd/issues/427
(defn main-panel []
  [:div.min-vh-100.bg-cyan-400.overflow-auto
   ;; stylesheet link must be here, not in other component
   [:link {:rel "stylesheet" :href "react-datepicker.css"}]
   [:div.min-vh-100.m-4
    [item-input]
    [items-view]
    [sleep-view]
    [chart-view]]])
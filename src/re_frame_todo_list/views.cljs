(ns re-frame-todo-list.views
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.subs :as subs]
   [re-com.core   :refer [at h-box v-box box gap line label title slider checkbox input-text horizontal-bar-tabs vertical-bar-tabs p]]
   [re-com.slider :refer [slider-parts-desc slider-args-desc]]
   [reagent.core  :as    reagent]
   ["react-beautiful-dnd" :as react-beautiful-dnd]
   ["recharts" :as recharts]
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

(defn item-view
  [index item]
  [:div.row.rounded.bg-cyan-300.p-1
   [:div.col-6.p-0.bg-cyan-300
    (let [id (str "super-unique-string" (:id item))]
      [:input.w-100.rounded.p-1.bg-cyan-200
       {:type "text"
        :id id
        :value (:val item)
        ;; perhaps this :on-change should filter newline
        :on-change #(re-frame/dispatch [:edit-item index (-> % .-target .-value)])
        :on-key-press #(if (= 13 (.-charCode %))
                         (.blur (.getElementById js/document id)))}])]
   (doall
    (map
     (fn [i]
       (let [date (keyword (.toDateString (js/Date. (+ (js/Date.now) (* 1000 60 60 24 i)))))]
         [:div.col
          {:style {:cursor :pointer}
           :key (str i "specisl" index)
           :on-click #(do (.preventDefault %)
                          (re-frame/dispatch [:mark-item-date-yes index date]))}
          (let [status (get-in item [:done date])
                elements {true [:i.fa.fa-check]
                          false [:i.fa.fa-x]
                          nil [:i.fa.fa-question]}]
            (get elements status))]))
     (range 0 -5 -1)))
   [:div.col [:i.fa.fa-trash
                {:style {:cursor :pointer}
                 :on-click #(re-frame/dispatch [:delete-item index])}]]])

(defn chart-view
  []
  [line-chart
   {:height 300 :width 500}
   [cartesian-grid
    {:stroke-dash-array "3 3"}]
   [xaxis]
   [yaxis]])

(defn items-header
  []
  [:h2.text-center.text-white "Your Habits"])

;; maybe height should be a component-local reagent atom
(defn items-view
  []
  (let [items (re-frame/subscribe [::subs/items])
        selected-item (re-frame/subscribe [::subs/selected-item])
        moving? (reagent/atom nil)
        panel-dnd-id "todo-list-dnd"]
    [:div
     [items-header]
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

;; https://github.com/atlassian/react-beautiful-dnd/issues/427
(defn main-panel []
  [:div.min-vh-100.bg-info.overflow-auto
   [:div.min-vh-100.m-4
    [item-input]
    [items-view]
    [chart-view]]])
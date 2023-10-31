(ns re-frame-todo-list.views
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.subs :as subs]
   [re-com.core   :refer [at h-box v-box box gap line label title slider checkbox input-text horizontal-bar-tabs vertical-bar-tabs p]]
   [re-com.slider :refer [slider-parts-desc slider-args-desc]]
   [reagent.core  :as    reagent]
   ["react-beautiful-dnd" :as react-beautiful-dnd]
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

(defn item-view
  [index item]
  [:div.row
   {:style {:background-color :gray}}
   [:div.col-1
    [:i.fa-solid.fa-ellipsis-vertical
     {:style {:cursor :pointer}
      :on-mouse-down (fn [e] (do (.preventDefault e)
                                 (re-frame/dispatch [:lift index e])))}]]
   [:div.col-5
    (let [id (str "super-unique-string" (:id item))]
      [:input
       {:type "text"
        :id id
        :style {:background-color :gray}
        :value (:val item)
        ;; perhaps this :on-change should filter newline
        :on-change #(re-frame/dispatch [:edit-item index (-> % .-target .-value)])
        :on-key-press #(if (= 13 (.-charCode %))
                         (.blur (.getElementById js/document id)))}])]
   (doall
    (map
     (fn [i]
       (let [date (keyword (.toDateString (js/Date. (+ (js/Date.now) (* 1000 60 60 24 i)))))]
         [:div.col-1
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
   [:div.col-1 [:i.fa.fa-trash
                {:style {:cursor :pointer}
                 :on-click #(re-frame/dispatch [:delete-item index])}]]])

;; maybe height should be a component-local reagent atom
(defn items-view
  []
  (let [items (re-frame/subscribe [::subs/items])
        selected-item (re-frame/subscribe [::subs/selected-item])
        moving? (reagent/atom nil)
        panel-dnd-id "todo-list-dnd"]
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
            [:h2 "My List - render some draggables inside"
             (doall
              (map-indexed
               (fn [index item]
                 ^{:key (:id item)}
                 [:div.row
                  ;;{:style {:margin 10}}
                  [draggable {:draggable-id (str (:id item)), :index index}
                   (fn [provided snapshot]
                     (reagent/as-element
                      [:div (merge {:ref (.-innerRef provided)}
                                   (js->clj (.-draggableProps provided))
                                   (js->clj (.-dragHandleProps provided)))
                       [item-view index item]]))]])
               items))]
            (.-placeholder provided)])))]]))

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
  [:div
   [item-input]
   [items-view]])
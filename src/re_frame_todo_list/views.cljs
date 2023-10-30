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

; https://stackoverflow.com/questions/33446913/reagent-react-clojurescript-warning-every-element-in-a-seq-should-have-a-unique
; https://stackoverflow.com/questions/24239144/js-console-log-in-clojurescript

(def drag-drop-context (reagent/adapt-react-class react-beautiful-dnd/DragDropContext))
(def droppable (reagent/adapt-react-class react-beautiful-dnd/Droppable))
(def draggable (reagent/adapt-react-class react-beautiful-dnd/Draggable))

(defn item-view
  [index item]
  [:div.row
   {:style {:margin 10
            :background-color :gray}}
   [:div.col-1
    [:i.fa-solid.fa-ellipsis-vertical
     {:style {:cursor :pointer}
      :on-mouse-down (fn [e] (do (.preventDefault e)
                                 (re-frame/dispatch [:lift index e])))}]]
   [:div.col-10 [:p (:val item)]]
   [:div.col-1 [:i.fa.fa-trash
                {:style {:cursor :pointer}
                 :on-click #(re-frame/dispatch [:delete-item index])}]]])

;; maybe height should be a component-local reagent atom
(defn items-view
  []
  (let [items (re-frame/subscribe [::subs/items])
        selected-item (re-frame/subscribe [::subs/selected-item])
        moving? (reagent/atom nil)]
    (fn
    []
    [:div
     [:div {:style {:position :absolute}} "moving? " (str @moving? " " @selected-item)]
     (if (= 0 (count @items))
       [:div "You've got nothing to do!"]
       [:div.container
        ;; Example droppable (wraps one of your lists)
        ;; Note use of r/as-element and js->clj on droppableProps
        #_[droppable {:droppable-id "droppable-1" :type "thing"}
         (fn [provided snapshot]
           (reagent/as-element [:div (merge {:ref   (.-innerRef provided)
                                             :class (when (.-isDraggingOver snapshot) :drag-over)}
                                            (js->clj (.-droppableProps provided)))
                                [:h2 "My List - render some draggables inside"]
                                (.-placeholder provided)]))]
                                        ; Example draggable
        #_[draggable {:draggable-id "draggable-1", :index 0}
         (fn [provided snapshot]
           (reagent/as-element [:div (merge {:ref (.-innerRef provided)}
                                            (js->clj (.-draggableProps provided))
                                            (js->clj (.-dragHandleProps provided)))
                                [:p "Drag me"]]))]])])))

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
       :on-key-press (fn [e] (if (= 13 (.-charCode e)) (add-item) ))
       }]
     [:input
      {:type "button"
       :value "Add item"
       :on-click add-item}]]))

(defn main-panel []
  (let [items (re-frame/subscribe [::subs/items])
        selected-item (re-frame/subscribe [::subs/selected-item])
        si @selected-item
        new-item (re-frame/subscribe [::subs/new-item])]
    ;; Example drag-drop-context (typically wraps your whole app)
    [drag-drop-context
     {:onDragStart  #()
      :onDragUpdate #()
      :onDragEnd    #()}
     [:div
      [droppable {:droppable-id "droppable-1" :type "thing"}
       (fn [provided snapshot]
         (reagent/as-element [:div (merge {:ref   (.-innerRef provided)
                                     :class (when (.-isDraggingOver snapshot) :drag-over)}
                                    (js->clj (.-droppableProps provided)))
                        [:h2 "My List - render some draggables inside"
                         [draggable {:draggable-id "draggable-1", :index 0}
                          (fn [provided snapshot]
                            (reagent/as-element [:div (merge {:ref (.-innerRef provided)}
                                                       (js->clj (.-draggableProps provided))
                                                       (js->clj (.-dragHandleProps provided)))
                                           [:p "Drag me"]]))]]
                        (.-placeholder provided)]))]]]))
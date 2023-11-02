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
   ["react-router-dom" :refer (BrowserRouter Routes Route Outlet Link)]
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

(def browser-router (reagent/adapt-react-class BrowserRouter))
(def routes (reagent/adapt-react-class Routes))
(def route (reagent/adapt-react-class Route))
(def outlet (reagent/adapt-react-class Outlet))
(def link (reagent/adapt-react-class Link))

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
  (let [data (re-frame/subscribe [::subs/sleep-data])]
    (fn []
      [line-chart
       {:height 300 :width 500 :data (clj->js @data)}
       ;; why oh why do the props to recharts need to be camelcase? the props for
       ;; rbd are in kebab case...
       [cartesian-grid
        {:strokeDasharray "3 3"}]
       [xaxis {:dataKey "name"}]
       [yaxis]
       [rechart-line {:type "monotone" :dataKey "time-ms" }]])))

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

(defn add-sleep-input-selector
  [time-atom touched-atom]
  [datepicker {:showTimeSelect true
               :selected @time-atom
               :on-change #(do
                             (reset! time-atom %)
                             (reset! touched-atom true))
               :showIcon true
               :timeIntervals 5
               :dateFormat "MM/dd/yyyy h:mm aa"}])

(defn add-sleep-input
  []
  (let [start (reagent/atom nil)
        stop (reagent/atom nil)
        start-touched (reagent/atom nil)
        stop-touched (reagent/atom nil)
        sleep-history (re-frame/subscribe [::subs/sleep-history])
        sleep-data (re-frame/subscribe [::subs/sleep-data])]
    (fn []
      [:div
       [:p "Sleep history: " (str @sleep-history)]
       [:p "Sleep data: " (str @sleep-data)]
       [:p "Add time slept"]
       [add-sleep-input-selector start start-touched]
       [add-sleep-input-selector stop stop-touched]
       [:input {:type "button"
                :value "Add interval"
                :disabled (or (not @start-touched) (not @stop-touched))
                :on-click #(do
                             (re-frame/dispatch [:add-sleep-interval @start @stop])
                             (reset! start nil)
                             (reset! stop nil)
                             (reset! start-touched nil)
                             (reset! stop-touched nil))}]])))

(defn sleep-view
  []
  (let [date (reagent/atom (js/Date.))]
    (fn []
      [datepicker {:showTimeSelect true
                   :selected @date
                   :on-change #(reset! date %)
                   :showIcon true
                   :timeIntervals 5
                   :dateFormat "MM/dd/yyyy h:mm aa"}])))



;; https://github.com/atlassian/react-beautiful-dnd/issues/427
(defn main-panel
  []
  (let [;router
        #_(reagent/atom (createBrowserRouter (clj->js [{:path "/"
                                                      :element (reagent/create-element "div" #js{} "Hello world")}])))]
    (fn []
      [browser-router
       [:div "This is the header, below which render links"]
       [routes
        [route {:path "/" :element (reagent/create-element "div" #js{} ""
                                     (reagent/as-element [:div
                                                          [link {:to "/"} "Home"]
                                                          [link {:to "/index"} "Index"]
                                                          [link {:to "/input"} "Input"]
                                                          [outlet]]))}
         [route {:path "index" :element (clj->js (reagent/create-element "div" #js{} "This is the Index content"))}]
         [route {:path "input" :element (reagent/create-element "div" #js{} "This is the Input content")}]]]]
      #_[router-provider {:router @router}]
      #_[:div.min-vh-100.bg-cyan-400.overflow-auto
       ;; stylesheet link must be here, not in other component
       [:link {:rel "stylesheet" :href "react-datepicker.css"}]
       [:div.min-vh-100.m-4
        [item-input]
        [items-view]
        [sleep-view]
        [add-sleep-input]
        [chart-view]]])))
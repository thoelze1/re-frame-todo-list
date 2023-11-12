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
    [:div.text-black
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

(defn datepicker-example
  []
  (let [date (reagent/atom (js/Date.))]
    (fn []
      [datepicker {:showTimeSelect true
                   :selected @date
                   :on-change #(reset! date %)
                   :showIcon true
                   :timeIntervals 5
                   :dateFormat "MM/dd/yyyy h:mm aa"}])))

(defn router-example
  []
  [browser-router
   [:div "This is the header, below which render links"]
   [routes
    [route {:path "/"
            :element (reagent/as-element [:div
                                          [link {:to "/"} "Home"]
                                          [link {:to "/index"} "Index"]
                                          [link {:to "/input"} "Input"]
                                          [outlet]])}
     [route {:path "index"
             :element (reagent/as-element [:div "This is the Index content"])}]
     [route {:path "input"
             :element (reagent/as-element [:div "This is the Input content"])}]]]])

(defn timeline-view
  []
  [:button {:on-click #(re-frame/dispatch [:helper])} "Test"])

(defn todo-view
  []
  [:div
   [item-input]
   [items-view]])

(defn sleep-view
  []
  [:div
   [add-sleep-input]
   [chart-view]])

;; This tool is not meant to be a full-fledged expense tracker. The UI wants to
;; be minimal to reconcile the expenses that you know you've made throughout
;; your day with your cash. There are other expense types such as subscription
;; payments that are different: you didn't initiate the payment in the
;; moment. So what even is the point of this? It's good for any small enough
;; transactions where you don't care to get a receipt. But even if you do get a
;; receipt, it's nice to have a single source of truth for all of your
;; transactions, whether you used a card, have a receipt, used cash, etc. In a
;; sense, payents that you initiate in the moment are fundamentally different
;; from subscriptions, which form a different category. But are there other
;; categories? The other category might just be fraud. Then there are "IOUs"
;; where you don't pay someone in the moment, or where someone owes you. These
;; are also good to track...

(def expense-fields
  {:timestamp {:label "Date & Time"
               :selector (fn [atom]
                           [datepicker {:showTimeSelect true
                                        :selected (deref atom)
                                        :on-change #(reset! atom %)
                                        :showIcon true
                                        :timeIntervals 5
                                        :dateFormat "MM/dd/yyyy h:mm aa"
                                        :className "rounded"}])
               :view identity
               :default (js/Date.)}
   :name {:label "Expense Name"
          :selector (fn [atom]
                      [:input.rounded.px-2
                       {:type "text"
                        :value (deref atom)
                        :on-change #(reset! atom (-> % .-target .-value))}])
          :view identity
          :default ""}
   :currency {:label "Currency"
              :selector (fn [atom]
                          [:select.rounded.px-2
                           {:on-change (fn [e]
                                         (reset! atom (-> e (.-target) (.-value))))}
                           [:option "USD"]
                           [:option "ARS"]])
              :view identity
              :default "USD"}
   :amount {:label "Amount"
            :selector (fn [atom]
                        [:input.rounded.px-2
                         {:type "number"
                          :value (deref atom)
                          :on-change #(reset! atom (-> % .-target .-value))}])
            :view identity
            :default "0"}})

;; I'll replace this if I find a builtin for mapping over values
(defn map-vals [f m]
  (reduce (fn [new [k v]]
            (conj new [k (f v)]))
          {}
          m))

;; you have to be careful how you use do's when using for with atoms
(defn add-expense-input
  []
  (let [atoms (map-vals #(reagent/atom (get % :default)) expense-fields)]
    (fn []
      [:div.row
       (doall
        (for [[field-key field-map] expense-fields]
          [:div.col
           [:p (:label field-map)]
           [:div.text-black
            (apply (:selector field-map) [(get atoms field-key)])]]))
       [:div.col
        [:input
         {:type "button"
          :value "Add expense"
          :on-click #(do
                       (re-frame/dispatch [:add-expense (map-vals deref atoms)])
                       (dorun
                        (for [[k v] atoms]
                          (reset! v (get-in expense-fields [k :default])))))}]]])))

(defn expenses-list-view
  []
  (let [items (re-frame/subscribe [::subs/expenses])]
    [:div
     (for [item-map @items]
       [:div.row.bg-cyan-500.rounded
        (for [[field-key field-map] expense-fields]
          [:div.col.text-center
           (str (:label field-map) ": " (apply (get field-map :view)
                                               [(get item-map field-key)]))])])]))

(defn expenses-view
  []
  [:div
   [:p.text-center.text-white.text-xl.font-mono.m-2 "Expenses"]
   ;;[add-currency-input]
   ;;[add-payment-method-input]
   [add-expense-input]
   [expenses-list-view]])

(def pages
  {:timeline {:title "Timeline"
              :component timeline-view}
   :todo {:title "To-Do"
          :component todo-view}
   :sleep {:title "Sleep Habits"
           :component sleep-view}
   :expenses {:title "Expenses"
              :component expenses-view}})

 ;; https://github.com/atlassian/react-beautiful-dnd/issues/427
(defn main-panel
  []
  (let [page (reagent/atom :expenses)
        set-page #(reset! page %)]
    (fn []
      [:div.min-vh-100.bg-cyan-400.overflow-auto.text-white.font-mono
       ;; stylesheet link must be here, not in other component
       [:link {:rel "stylesheet" :href "react-datepicker.css"}]
       [:div.min-vh-100.m-4
        [:p.text-center.text-white.text-3xl.font-mono.m-2 "Your Everything Tracker"]
        [:div.row.bg-cyan-500.rounded
         (for [[k m] pages]
           [:div.col.text-center
            [:button.px-2.rounded.m-1
             {:on-click #(set-page k)
              :disabled (= k @page)
              :class (if (= k @page) "bg-cyan-600")}
             [:p (:title m)]]])]
        [(get (pages @page) :component)]]])))
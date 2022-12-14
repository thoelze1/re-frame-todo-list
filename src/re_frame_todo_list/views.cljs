(ns re-frame-todo-list.views
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.subs :as subs]
   [re-com.core   :refer [at h-box v-box box gap line label title slider checkbox input-text horizontal-bar-tabs vertical-bar-tabs p]]
   [re-com.slider :refer [slider-parts-desc slider-args-desc]]
   [reagent.core  :as    reagent]
   [re-frame-todo-list.macros :as macros]
   [cljsjs.react-flip-move]
   ))

;; the todo items should comprise a priority level; therefore this priority
;; level (which I'm using as `height` or `idx`) should be stored in the db the
;; priority level is a data structure abstraction; the "height" is simply a
;; visual abstraction. therefore the height related information should live in
;; the view context, and perhaps passed as necessary to the events context


; https://stackoverflow.com/questions/33446913/reagent-react-clojurescript-warning-every-element-in-a-seq-should-have-a-unique
; https://stackoverflow.com/questions/24239144/js-console-log-in-clojurescript

(def flip-move (reagent/adapt-react-class js/FlipMove))

(def initial 0)
(def multiple 60)

(defn idx->height
  [idx]
  (+ initial (* multiple idx)))

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
        selected-item (re-frame/subscribe [::subs/selected-item])]
    [:div
     [:div {:style {:position :absolute}} "hi"]
     (if (= 0 (count @items))
       [:div "You've got nothing to do!"]
       [:div.container
        {:style {:position :relative}}
        ;; This was an attempt to re-draw the dragged element! Makes much more
        ;; sense to directly manipulate the element drawn by flip-move
        (if @selected-item
          (do #_(println "item: " (str (js/document.getElementById (str @selected-item))))
              (let [h (-> js/document
                          (.getElementById (str (get-in @items [@selected-item :id])))
                          (.getBoundingClientRect)
                          (.-height))]
                (do #_(println (str h))
                    [:div 
                     (let [item (get @items @selected-item)]
                       [:div.row {:style {:z-index 10000
                           ;:position :absolute
                           :flex-wrap "wrap"
                           ;:display "flex"
                           ;:flex-direction "row"
                                          }}
                        [:div.row {:style {:position :absolute}}
                         [:div.row {:style {:position :absolute
                                            :top (:height item)}}
                          [item-view @selected-item item]]]])]))))
        
        [flip-move {:duration 500
                    :easing "cubic-bezier(0.6, -0.28, 0.735, 0.045)"}
         (doall
          (map-indexed
           (fn [index item]
             ^{:key (:id item)}
             [:div.row
              
              {:style {:visibility (if (= index @selected-item) :hidden)
                       ;;:position (if (= index @selected-item) :absolute)
                       ;; if we manually track all the heights, then that means
                       ;; that deletion becomes a constant time operation as we
                       ;; have to manually go through and change all the
                       ;; subsequent heights. Instead, we'll specially render the
                       ;; dragged item and the displaced item. For now we'll store
                       ;; the height information in the items list, though we
                       ;; could store it directly in the app-db
                       :top (if (= index @selected-item)
                                     (:height item)
                                     0 ;(idx->height index)
                                     )
                       :z-index (if (= index @selected-item) 1 0)}
               :key (:id item)
               :id (str (:id item))}
              [item-view index item]])
           @items))]])]))

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


(defn slider-demo
  []
  (let [slider-val  (reagent/atom "0")
        slider-min  (reagent/atom "0")
        slider-max  (reagent/atom "100")
        slider-step (reagent/atom "1")
        disabled?   (reagent/atom false)]
    (fn
      []
      [:div
       [slider
        :src       (at)
        :model     slider-val
        :min       slider-min
        :max       slider-max
        :step      slider-step
        :width     "300px"
        :on-change #(reset! slider-val (str %))
        :disabled? disabled?]
       [:div
        [title :src (at) :level :level3 :label "Interactive Parameters" :style {:margin-top "0"}]
        [h-box
           :src      (at)
           :gap      "10px"
           :align    :center
           :children [[box
                       :src      (at)
                       :align :start
                       :width "60px"
                       :child [:code ":val"]]
                      [input-text
                       :src      (at)
                       :model           slider-val
                       :width           "60px"
                       :height          "26px"
                       :on-change       #(reset! slider-val %)
                       :change-on-blur? false]]]
        [h-box
           :src      (at)
           :gap      "10px"
           :align    :center
           :children [[box
                       :src      (at)
                       :align :start
                       :width "60px"
                       :child [:code ":min"]]
                      [input-text
                       :src      (at)
                       :model           slider-min
                       :width           "60px"
                       :height          "26px"
                       :on-change       #(reset! slider-min %)
                       :change-on-blur? false]]]
        [h-box
           :src      (at)
           :gap      "10px"
           :align    :center
           :children [[box
                       :src      (at)
                       :align :start
                       :width "60px"
                       :child [:code ":max"]]
                      [input-text
                       :src      (at)
                       :model           slider-max
                       :width           "60px"
                       :height          "26px"
                       :on-change       #(reset! slider-max %)
                       :change-on-blur? false]]]
        [h-box
           :src      (at)
           :gap      "10px"
           :align    :center
           :children [[box
                       :src      (at)
                       :align :start
                       :width "60px"
                       :child [:code ":step"]]
                      [input-text
                       :src      (at)
                       :model           slider-step
                       :width           "60px"
                       :height          "26px"
                       :on-change       #(reset! slider-step %)
                       :change-on-blur? false]]]
        [checkbox
         :src      (at)
         :label [box
                 :src      (at)
                 :align :start :child [:code ":disabled?"]]
         :model disabled?
         :on-change (fn [val]
                      (reset! disabled? val))]]])))


(defn main-panel []
  (let [items (re-frame/subscribe [::subs/items])
        selected-item (re-frame/subscribe [::subs/selected-item])
        si @selected-item
        new-item (re-frame/subscribe [::subs/new-item])]
    [:div.container
     [:div.row]
     #_[:div.row
      [:p (str @selected-item)]
      [:p (str @items)]]
     [item-input]
     [items-view]]))

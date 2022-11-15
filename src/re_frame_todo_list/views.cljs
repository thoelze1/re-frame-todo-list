(ns re-frame-todo-list.views
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.subs :as subs]
   [re-com.core   :refer [at h-box v-box box gap line label title slider checkbox input-text horizontal-bar-tabs vertical-bar-tabs p]]
   [re-com.slider :refer [slider-parts-desc slider-args-desc]]
   [reagent.core  :as    reagent]
   [re-frame-todo-list.macros :as macros]
   ))

; https://stackoverflow.com/questions/33446913/reagent-react-clojurescript-warning-every-element-in-a-seq-should-have-a-unique
; https://stackoverflow.com/questions/24239144/js-console-log-in-clojurescript
(defn items-view
  []
  (let [z (reagent/atom 0)]
    (fn []
      (let [items (re-frame/subscribe [::subs/items])]
        (if (= 0 (count @items))
          [:div "You've got nothing to do!"]
          [:div.container
                                        ;{:position :absolute}
           (doall
            (map-indexed
             (fn [index item]
               ^{:key (:val item)}
               [:div.row
                [:div.row
                 {:style {:position :absolute
                          :background-color :gray
                          :top (:height item)
                          :z-index (:z item)}}
                 [:div.col-1
                  [:i.fa-solid.fa-ellipsis-vertical
                   {:style {:cursor :pointer}
                    :on-mouse-down (fn [e] (do (swap! z inc)
                                             (.preventDefault e)
                                               (re-frame/dispatch [:drag index e @z])))}]]
                 [:div.col-10 (:val item)]
                 [:div.col-1 [:i.fa.fa-trash
                              {:style {:cursor :pointer}
                               :on-click #(re-frame/dispatch [:delete-item index])}]]]])
             @items))])))))

(defn item-input
  []
  (let [new-item (re-frame/subscribe [::subs/new-item])
        gettext (fn [e] (-> e .-target .-value))
        touch    (fn [e] (re-frame/dispatch [:new-item (gettext e)]))
        add-item #(do
                    (re-frame/dispatch [:add-item @new-item])
                    (re-frame/dispatch [:new-item ""]))]
    [:div
     [:input
      {:type "text"
       :value @new-item
       ; perhaps this :on-change should filter newline
       :on-change touch
       :on-key-press (fn [e] (if (= 13 (.-charCode e)) (add-item)))}]
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
  [:div
   {:style {:background-color :yellow}}
   [item-input]
   [items-view]
   [:div
     {:style {:background :gray
              :padding 0}}
    [:span {:style {:position :relative
                    :bottom 10}}"hi"]]])

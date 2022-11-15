(ns re-frame-todo-list.views
  (:require
   [re-frame.core :as re-frame]
   [re-frame-todo-list.subs :as subs]
   ))

; https://stackoverflow.com/questions/33446913/reagent-react-clojurescript-warning-every-element-in-a-seq-should-have-a-unique
(defn items-view
  []
  (let [items (re-frame/subscribe [::subs/items])]
    [:ul
     (for [item @items]
       ^{:key item} [:li item])]))

(defn item-input
  []
  (let [new-item (re-frame/subscribe [::subs/new-item])
        gettext (fn [e] (-> e .-target .-value))
        emit    (fn [e] (re-frame/dispatch [:new-item (gettext e)]))]
    [:div
     [:input
      {:type "text"
       :value @new-item
       :on-change emit}]
     [:input
      {:type "button"
       :value "Add item"
       :on-click #(do
                    (re-frame/dispatch [:add-item @new-item])
                    (re-frame/dispatch [:new-item ""]))}]]))

(defn main-panel []
  [:div
   [item-input]
   [items-view]])

(ns re-frame-todo-list.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [re-frame-todo-list.events :as events]
   [re-frame-todo-list.views :as views]
   [re-frame-todo-list.config :as config]
   [day8.re-frame.http-fx]
   [lambdaisland.glogi :as log]
   [lambdaisland.glogi.console :as glogi-console]))

(glogi-console/install!)

(log/set-levels
 {:glogi/root   :info})

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [:app/connect])
  (dev-setup)
  (mount-root))

(ns re-frame-todo-list.db)

(def default-db
  {:new-item ""
   :items []
   :selected-item nil
   :sleep-history {}
   :sleep-data {}})

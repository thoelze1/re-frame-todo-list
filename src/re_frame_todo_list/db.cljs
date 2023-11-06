(ns re-frame-todo-list.db)

(def default-db
  {:new-item ""
   :items []
   :selected-item nil
   :sleep-history {}
   :sleep-data {}
   :expenses {:currencies []
              :payment-methods []
              :expense-list [{:timestamp (js/Date.)
                              :name "Lunch with Aiden"
                              :currency "ARG"
                              :amount 5800}]}})

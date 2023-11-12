; https://code.thheller.com/blog/shadow-cljs/2019/10/12/clojurescript-macros.html

(ns re-frame-todo-list.macros)

(defmacro unquotee [var]
  (apply (fn [e] `(fn [] ~e)) (list var)))

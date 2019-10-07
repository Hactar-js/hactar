(ns hactar.projects)

(defmacro defprojectwatcher [name key cb]
  `(add-watch hactar.projects.project ~name
             (fn [_ _ _ new-state]
               (if (contains? new-state ~key)
                 (~cb (get-in new-state ~key))))))
                

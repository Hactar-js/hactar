(ns hactar.behaviors)

(defmacro defbehavior [behavior-name args & body]
  `(hactar.behaviors.add-behavior ~(keyword behavior-name) ~(meta behavior-name) (fn ~args ~@body)))

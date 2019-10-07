(ns hactar.commands)

(defmacro defcommand [cmd-name args & body]
  `(hactar.commands.add-command (name ~cmd-name) (fn ~args ~@body)))

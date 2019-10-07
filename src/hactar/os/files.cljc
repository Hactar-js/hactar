(ns hactar.os.files)

(defmacro deffilewatcher [args & body]
  "Watches for a file being added"
  `(.on hactar.os.files.watcher "add" (fn ~args ~@body)))

(ns hactar.os.shell
  (:require
   [cljs.core.async :refer [chan put! close!]]
   ["child_process" :as cp]))

(defn exec
  "run a js function that returns stdout/stderr"
  [cmd]
  (let [out (chan)]
    (try
      (let [result (cp/execSync cmd #js {:stdio "inherit"})]
        (put! out :done))
      (catch js/Error e
        (put! out (ex-message e))))
    out))

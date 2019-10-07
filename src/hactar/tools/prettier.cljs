(ns hactar.tools.prettier
  (:require ["prettier" :as p]
            ["path" :as path]
            [cljs.core.async :refer [chan put!]]))

(defn prettify [text]
  (let [out (chan)
        cwd (js/process.cwd)]
    (-> (p/resolveConfig (.join path cwd "package.json"))
        (.then (fn [options]
                 (let [options (or options #js {})]
                   (put! out (p/format text (js/Object.assign options #js {:parser "babel"})))))))
    out))

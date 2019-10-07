(ns hactar.config
  (:require [cljs.reader :as edn]
            [mount.core :as mount :refer-macros [defstate]]
            [cljs.core.async :refer [<! close!] :refer-macros [go-loop]]
            [hactar.behaviors :refer [has-behavior? run-behavior]]
            [hactar.os.files :refer [slurp watch-change]]))

(def config (atom {}))

(defonce hactar-config-changes> (watch-change "./hactar.edn"))

(defn read-config [path]
  (let [_config (edn/read-string (slurp path))]
    (swap! config merge _config))
  @config)

(defn start []
  (go-loop []
    (let [path (<! hactar-config-changes>)
          config (read-config path)]
      (doseq [kv config]
        (let [behavior (key kv)
              config (val kv)]
          (if (has-behavior? behavior)
            (run-behavior behavior config)))))
    (recur)))

(defstate config-state :start (start)
                       :stop (close! @config-state))

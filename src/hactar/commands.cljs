(ns hactar.commands
  (:require-macros [hactar.commands])
  (:require [mount.core :as mount :refer [defstate]]
            ["minimist" :as minimist]
            ["command-line-commands" :as clc]))

(def commands (atom {}))
(def valid-commands (atom []))

(defn add-command [cmd-name cmd]
  (swap! commands assoc cmd-name cmd)
  (swap! valid-commands conj cmd-name))

(defn start []
  (try
    (let [{command :command argv :argv} (js->clj (clc (clj->js @valid-commands)) :keywordize-keys true)
          cmd (get @commands command)]
      (when cmd
        (cmd (js->clj (minimist (clj->js argv)) :keywordize-keys true))))
    (catch js/Error e)))
      

(defstate commands-state :start (start)
                         :stop ())


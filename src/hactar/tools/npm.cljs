(ns hactar.tools.npm
  (:require
   [clojure.string :refer [join]]
   [cljs.core.async :refer [chan put!] :refer-macros [go go-loop]]
   [full.async :refer-macros [go-try <?]]
   [hactar.project :as project]
   [hactar.os.files :refer [file-exists? aslurp aspit]]
   [hactar.os.shell :refer [exec]]))

(def config-path "./package.json")
(defn installed? [package]
  (project/has-dep? (keyword package)))

(defn initialized? []
  (file-exists? config-path))

(defn dump-config [config]
  (aspit config-path config))

(defn init []
  (let [out (chan)]
    (if (initialized?)
      (put! out :done)
      (go
        (try
          (let [result (<? (exec "yarn init --yes"))]
            (put! out result))
          (catch js/Error e
            (let [message (ex-message e)]
              (put! out e)
              (js/console.log (str "Could not init")))))))
    out))

(defn add-scripts [scs]
  (let [out (chan)
        scripts (clj->js scs)]
    (go
      (try
        (let [initialized (<? (init))
              config-raw (<? (aslurp config-path))
              config (.parse js/JSON config-raw)
              _ (if-not (.-scripts config)
                  (set! (.-scripts config) scripts)
                  (set! (.-scripts config) (js/Object.assign (.-scripts config) scripts)))
              dumped (<? (dump-config (.stringify js/JSON config nil 2)))]
          (put! out dumped))))
    out))

(defn install [& packages]
  (js/console.log (str "installing: " (join " " packages)))
  (let [out (chan)
        result-stream (exec (str "yarn add " (join " " packages)))]
    (go-loop []
      (try
        (let [result (<? result-stream)]
          (if (= result :done)
            (do
              (js/console.log (str "installed: " (join " " packages)))
              (put! out result)
              result)
            (do
               (js/console.log result)
               (recur))))
        (catch js/Error e
          (let [message (ex-message e)]
            (put! out e)
            (js/console.log (str "Could not install: " message))))))
    out))


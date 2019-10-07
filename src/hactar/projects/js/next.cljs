(ns hactar.projects.js.next
  (:require
   ["os" :refer [homedir]]
   [clojure.string :refer [capitalize replace upper-case]]
   [cljs.core.async :refer [put! chan] :refer-macros [go]]
   [full.async :refer-macros [<?]]
   ["jscodeshift" :as j]
   [oops.core :refer [oget]]
   [hactar.tools.npm :as npm]
   [hactar.tools.prettier :refer [prettify]]
   [hactar.os.files :refer [file-exists? aspit aslurp slurp]]))

(defonce config-path "./next.config.js")
(def base-config (slurp (str (homedir) "/.hactar/templates/js/next/next.config.js")))

;; TODO: Memoize this
(defn read-config-sync []
  (if (file-exists? config-path)
    (slurp config-path)
    base-config))

(defn add-default-config []
  (let [out (chan)]
    (go
      (try
        (let [installed (<? (npm/install "next-compose-plugins"
                                         "next" "react" "react-dom"))
              spitted (<? (aspit "./next.config.js" base-config))
              scripts-added (<? (npm/add-scripts {:dev "next"
                                                  :build "next build"
                                                  :start "next start"}))
              config (<? (aslurp config-path))]
          (put! out config))))
    out))

(defn read-config []
  (if (file-exists? config-path)
    (aslurp config-path)
    (add-default-config)))

(defn configured? [pkg]
  (let [pkg-match (re-pattern pkg)]
    (re-find pkg-match (read-config-sync))))

(defn filter-plugins [c]
  (try
    (= (oget c "value.callee.name") "withPlugins")
    (catch js/Error e
      false)))


(def filter-require (clj->js
                     {:declarations
                      [{:init
                        {:callee {:name "require"}
                         :arguments [{:type "Literal"}]}}]}))

(defn camelCase [s]
  (replace s #"-(\w)" #(upper-case (second %1))))

(defn make-require [name pkg-name]
  (str "const with" (capitalize (camelCase name)) " = require('" pkg-name "');"))

(defn make-config [name config]
  (str "[with" (capitalize (camelCase name)) ", {" config "}]"))

(defn generate-next-config [name pkg-name {:keys [next-config require config]}]
  (let [root (j next-config)
        v  (.-VariableDeclaration j) 
        reqs (.find root v filter-require)
        n (.-length reqs)
        a (.get (.at reqs (- n 1)))
        _ (.insertBefore (j a) require)
        _ (-> root
              (.find (.-CallExpression j))
              (.filter filter-plugins)
              (.find (.-ArrayExpression j))
              (.get "elements")
              (.push config))
        out (chan)]
    (go
      (let [formatted (<! (prettify (.toSource root)))
            result (<! (aspit config-path formatted))]
        (put! out result)))
    out))

(defn add-config [name pkg-name c & {:keys [require config]
                                     :or {require (make-require name pkg-name)
                                          config (make-config name c)}}]
  (let [out (chan)]
    (go
      (try
        (let [current-config (<? (read-config))
              result (<? (generate-next-config name pkg-name {:next-config current-config
                                                              :require require
                                                              :config config}))]
          (put! out result))
        (catch js/Error e
          (put! out e)
          (js/console.log (ex-message e)))))
    out))

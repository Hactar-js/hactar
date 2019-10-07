(ns main
  (:require ["process" :refer [exit]]
            ["os" :refer [homedir]]
            [clojure.string :refer [join]]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [full.async :refer-macros [<?]]
            [hactar.os.files :refer [copy]]
            [hactar.tools.npm :as npm]
            [hactar.tools.prettier :refer [prettify]]
            [hactar.logging :refer [red green blue yellow]]
            [hactar.projects.js.next :as next]
            [hactar.behaviors :as behaviors :refer [run-behavior list-behaviors] :refer-macros [defbehavior]]
            [hactar.config :as config]
            [hactar.commands :as commands :refer-macros [defcommand]]
            [mount.core :as mount]))

(mount/in-cljc-mode)

(defn log-error [err]
  (js/console.log err))

(defn log [a]
  (js/console.log a))

(defonce help-info "
    Usage
      $ hactar command
 
    Commands
      add: Add a behavior to a project
      help: prints this
      list: Returns a list of all the behaviors
 
    Examples
      $ hactar add next-mdx # adds mdx support to next project
")

(defcommand :help [argv]
  (js/console.log help-info)
  (exit))

(defcommand :add [argv]
  (let [behavior (-> argv (get :_) (first) (keyword))]
    (run-behavior behavior argv true)))

;; todo support stuff like searching by tags
(defcommand :list [argv]
  (doseq [[name props] (list-behaviors)]
    (log (str name " - " (get props :doc))))
  (exit))

(defonce next-react-svg-config "include: path.resolve(__dirname, 'src/assets/svg')")
(defbehavior ^{:doc "Adds svg support to next"} next-svg []
  (go
    (let [pkg "next-react-svg"]
      (if (not (npm/installed? pkg))
        (<! (npm/install pkg))
        (yellow (str "already installed: " pkg)))
      (if (not (next/configured? pkg))
        (let [result (<! (next/add-config "svg" pkg next-react-svg-config))]
          (if (= result :done)
            (green "added config for svg to next")
            (red "things went wrong")))
        (yellow (str "already configured: " "next-svg"))))))

(defonce next-mdx-config "pageExtensions: ['js', 'jsx', 'mdx'],")
(defonce next-mdx-require "const withMdx = require('@next/mdx')();")
(defbehavior ^{:doc "Adds mdx to a next project"} next-mdx []
  (go
    (let [pkgs ["@next/mdx" "@mdx-js/loader"]]
      (if-not (npm/installed? "@next/mdx")
        (<! (apply npm/install pkgs))
        (yellow (str "already installed: " (join " " pkgs))))
      (if-not (next/configured? "@next/mdx")
        (let [result (<! (next/add-config "mdx" "@next/mdx" next-mdx-config :require next-mdx-require))]
          (if (= result :done)
            (green "added config for mdx to next")
            (red "failed to add config for mdx to next")))))))

(def next-base-template-path (str (homedir) "/.hactar/templates/js/next/base"))
(defbehavior ^{:doc "Create a default next site"} next []
  (go
    (try
      (let [_ (<? (npm/init))
            _ (<? (next/add-default-config))
            _ (<? (copy next-base-template-path (js/process.cwd)))]
        (green "added default next config"))
      (catch js/Error e
        (red (ex-message e))))))

(defbehavior ^{:doc "Add css to a next site"} next-css []
  (go
    (try
      (let [pkgs ["@next/mdx"]
            _ (<? (npm/init))
            _ (<? (next/add-default-config))]))))

(defbehavior ^{:doc "Add post css variables"} postcss-vars []
  (go
    (try [])))

(defn reload! []
  (println "Code has been butted.")
  (mount/stop)
  (mount/start))

(defn main! []
  (mount/start))


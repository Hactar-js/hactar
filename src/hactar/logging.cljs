(ns hactar.logging
  (:require ["chalk" :as chalk]))

(defn red [s]
  (js/console.log (chalk/red s)))

(defn green [s]
  (js/console.log (chalk/green s)))

(defn blue [s]
  (js/console.log (chalk/blue s)))

(defn white [s]
  (js/console.log (chalk/white s)))

(defn yellow [s]
  (js/console.log (chalk/yellow s)))

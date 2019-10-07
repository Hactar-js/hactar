(ns hactar.behaviors
  (:require-macros [hactar.behaviors]))

(def behaviors (atom {}))

(defn has-behavior? [b]
  (contains? @behaviors b))

(defn list-behaviors []
  @behaviors)

(defn add-behavior [b-name meta behavior]
  (swap! behaviors assoc b-name (assoc meta :fn behavior)))

(defn run-behavior
  ([behavior-name props]
   (run-behavior behavior-name props false))
  ([behavior-name props exit-after-run]
   (let [b (get-in @behaviors [behavior-name :fn])]
     (go
       (try
         (let [result (<? (b props))]
           (when exit-after-run
             (js/process.exit)))
         (catch js/Error er
           (js/console.log err)))))))

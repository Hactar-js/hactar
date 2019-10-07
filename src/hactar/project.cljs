(ns hactar.project
  (:require [mount.core :as mount :refer-macros [defstate]]
            [cljs.core.async :refer [<! close!] :refer-macros [go-loop go]]
            [full.async :refer-macros [<?]]
            [hactar.os.files :refer [file-exists? watch-change aslurp slurp]]))

(def project (atom {:tags [] :dependencies {} :devDependencies {}}))

(defn has-tag?
  ([project tag]
   (contains? (get-in project [:tags]) tag))
  ([tag]
   (has-tag? @project tag)))

(defn add-tag [tag]
  (swap! project update-in [:tags] conj tag))

(defn has-dep?
  ([project dep]
   (or (contains? (get-in project [:devDependencies]) dep)
       (contains? (get-in project [:dependencies]) dep)))
  ([dep]
   (has-dep? @project dep)))

(defn add-package-json-details [{:keys [name version dependencies devDependencies]}]
  (swap! project assoc :name name)
  (swap! project assoc :version version)
  (swap! project assoc :dependencies dependencies)
  (swap! project assoc :devDependencies devDependencies))

(defn parse-package-json-sync []
  (let [json (slurp "./package.json")
        parsed-json (.parse js/JSON json)
        details (js->clj parsed-json :keywordize-keys true)]
    (add-package-json-details details)))

(defn parse-package-json [path]
  (go
    (try
      (let [json (<? (aslurp path))
            parsed-json (.parse js/JSON json)
            details (js->clj parsed-json :keywordize-keys true)]
        (add-package-json-details details))
      (catch js/Error e
        (js/console.log e)))))

(defonce package-json-changes (watch-change "./package.json"))

(defn start []
  (if (file-exists? "./package.json")
    (parse-package-json-sync)
    (go-loop []
      (let [path (<! package-json-changes)]
        (parse-package-json path))
      (recur))))

(defstate project-state :start (start)
                        :stop (close! @project-state))

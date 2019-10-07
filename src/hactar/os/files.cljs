(ns hactar.os.files
  (:require ["chokidar" :as ch]
            ["recursive-copy" :as rc]
            ["fs" :refer [readFile writeFile existsSync readFileSync]]
            [cljs.core.async :refer [chan put!]]))

(defn file-exists? [path]
  (existsSync path))

(defn aspit [path data]
  (let [out (chan)]
    (writeFile path data "utf8" (fn [err] (if err
                                            (put! out err)
                                            (put! out :done))))
    out))

(defn slurp [path]
  (readFileSync path "utf8"))

(defn aslurp [path]
  (let [out (chan)]
    (readFile path "utf8" (fn [err data] (if err
                                           (put! out err)
                                           (put! out data))))
    out))

(defn copy [src dest]
  (let [out (chan)]
    (rc src dest (fn [err results]
                   (if err
                     (put! out err)
                     (put! out results))))
    out))

(defn watch [path type]
  (let [out (chan)
        watcher (ch/watch path)]
    (.on watcher type #(put! out %))
    out))

(defn watch-add [path]
  (watch path "add"))

(defn watch-change [path]
  (watch path "change"))

(defn watch-removed [path]
  (watch path "unlink"))

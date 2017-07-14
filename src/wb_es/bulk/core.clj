(ns wb-es.bulk.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [mount.core :as mount]
            [wb-es.datomic.db :refer [datomic-conn]]
            [wb-es.env :refer [release-id]]))

(defn format-bulk
  "returns a new line delimited JSON based on
  an action name and a list of Documents (acoording to Document protocol)"
  [action-name documents]
  (->> documents
       (map (fn [doc]
              (if-not (= (name action-name) "delete")
                (format "%s\n%s"
                        (json/generate-string {action-name (.metadata doc)})
                        (json/generate-string (.data doc))))))
       (clojure.string/join "\n")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, Bulky World!"))

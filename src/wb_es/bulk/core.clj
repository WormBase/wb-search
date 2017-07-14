(ns wb-es.bulk.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [mount.core :as mount]
            [wb-es.datomic.db :refer [datomic-conn]]
            [wb-es.env :refer [es-base-url]]))

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
       (clojure.string/join "\n")
       (format "%s\n"))) ;trailing \n is necessary for Elasticsearch to parse the request

(defn submit
  "submit formatted new line delimited JSON to elasticsearch"
  [formatted-docs]
  (http/post (format "%s/_bulk" es-base-url)
             {:debug true
              :headers {:content-type "application/x-ndjson"}
              :body formatted-docs}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, Bulky World!"))

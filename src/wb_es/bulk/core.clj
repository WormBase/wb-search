(ns wb-es.bulk.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [datomic.api :as d]
            [mount.core :as mount]
            [wb-es.datomic.data.core :refer [create-document]]
            [wb-es.datomic.db :refer [datomic-conn]]
            [wb-es.env :refer [es-base-url release-id]]
            [wb-es.mappings.core :refer [index-settings]]))

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
             {:headers {:content-type "application/x-ndjson"}
              :body formatted-docs}))

(defn get-eids-by-type [db ident-attr]
  (d/q '[:find [?eid ...]
         :in $ ?ident-attr
         :where [?eid ?ident-attr]]
       db ident-attr))

(defn run-index-batch [db batch]
  (->> batch
       (map #(create-document (d/entity db %)))
       (format-bulk "index")
       (submit)))

(defn run []
  (let [db (d/db datomic-conn)
        index-url (format "%s/%s " es-base-url release-id)]
    (do
      (http/put index-url {:headers {:content-type "application/json"}
                           :body (json/generate-string index-settings)})
      (let [eids (get-eids-by-type db :go-term/id)
            step 100
            batches (partition step step [] eids)]
        (doseq [batch batches]
          (run-index-batch db batch))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (println "Hello, Bulky World!")
    (mount/start)
    (run)
    (mount/stop)))

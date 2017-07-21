(ns wb-es.bulk.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [>! <! >!! <!! go chan buffer close!]]
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

(defn get-eids-by-type
  "get all datomic entity ids of a given type
  indicated by its unique attribute ident
  such as :gene/id"
  [db ident-attr]
  (d/q '[:find [?eid ...]
         :in $ ?ident-attr
         :where [?eid ?ident-attr]]
       db ident-attr))

(defn make-batches
  "turn a list datomic entity ids to batches of the given size.
  attach some metadata for debugging"
  ([eids] (make-batches 500 nil))
  ([batch-size order-info eids]
   (->> eids
        (sort)
        (partition batch-size batch-size [])
        (map (fn [batch]
               (with-meta batch {:order order-info
                                 :size (count batch)
                                 :start (first batch)
                                 :end (last batch)}))))))

(defn run-index-batch
  "index data of a batch of datomic entity ids"
  [db batch]
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
      (let [n-threads 4
            scheduler (chan n-threads)
            logger (chan n-threads)]

        ;; logging
        (go
          (time
           (loop []
             (if-let [entry (<! logger)]
               ;; normal batches won't be nil
               ;; only get nil when channel is closed
               (do
                 (prn entry)
                 (recur))))))


        ;; multiple independent workers to execute jobs
        (dotimes [i n-threads]
          (go
            (loop []
              (if-let [job (<! scheduler)]
                ;; normal batches won't be nil
                ;; only get nil when channel is closed
                (do
                  (>! logger (or (meta job) :no_metadata))
                  (run-index-batch db job)
                  (recur))
                (close! logger)))))

        (do
          ;; add jobs to scheduler in sequence
          (let [eids (get-eids-by-type db :do-term/id)
                jobs (make-batches 1000 :do-term eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :feature/id)
                jobs (make-batches 1000 :feature eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :gene/id)
                jobs (make-batches 1000 :gene eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :go-term/id)
                jobs (make-batches 1000 :go-term eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :interaction/id)
                jobs (make-batches 1000 :interaction eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :molecule/id)
                jobs (make-batches 1000 :molecule eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :paper/id)
                jobs (make-batches 1000 :paper eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :phenotype/id)
                jobs (make-batches 1000 :phenotype eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :strain/id)
                jobs (make-batches 1000 :strain eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :variation/id)
                jobs (make-batches 1000 :variation eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          ;; close the channel to indicate no more input
          ;; existing jobs on the channel will remain available for consumers
          ;; https://clojure.github.io/core.async/#clojure.core.async/close!
          (close! scheduler))
        ))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (println "Hello, Bulky World!")
    (mount/start)
    (run)
    (mount/stop)))

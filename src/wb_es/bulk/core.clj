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
  ([action-name documents] (format-bulk action-name nil documents))
  ([action-name index documents]
   (->> documents
        (map (fn [doc]
               (if-not (= (name action-name) "delete")
                 (format "%s\n%s"
                         (json/generate-string {action-name (if index
                                                              (assoc (meta doc) :_index index) ; ie to specify test index
                                                              (meta doc))})
                         (json/generate-string doc)))))
        (clojure.string/join "\n")
        (format "%s\n")))) ;trailing \n is necessary for Elasticsearch to parse the request

(defn submit
  "submit formatted new line delimited JSON to elasticsearch"
  [formatted-docs & {:keys [refresh]}]
  (http/post (format "%s/_bulk?refresh=%s" es-base-url (or refresh "true"))
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
  ([eids] (make-batches 500 nil eids))
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

          (let [eids (get-eids-by-type db :analysis/id)
                jobs (make-batches 1000 :analysis eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :anatomy-term/id)
                jobs (make-batches 1000 :anatomy-term eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :antibody/id)
                jobs (make-batches 1000 :antibody eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :cds/id)
                jobs (make-batches 1000 :cds eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :clone/id)
                jobs (make-batches 1000 :clone eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :construct/id)
                jobs (make-batches 1000 :construct eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :expression-cluster/id)
                jobs (make-batches 1000 :expression-cluster eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :expr-pattern/id)
                jobs (make-batches 1000 :expr-pattern eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :expr-profile/id)
                jobs (make-batches 1000 :expr-profile eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
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
          (let [eids (get-eids-by-type db :gene-class/id)
                jobs (make-batches 1000 :gene-class eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :gene-cluster/id)
                jobs (make-batches 1000 :gene-cluster eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :go-term/id)
                jobs (make-batches 1000 :go-term eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :homology-group/id)
                jobs (make-batches 1000 :homology-group eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :interaction/id)
                jobs (make-batches 1000 :interaction eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :laboratory/id)
                jobs (make-batches 1000 :laboratory eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :life-stage/id)
                jobs (make-batches 1000 :life-stage eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :molecule/id)
                jobs (make-batches 1000 :molecule eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :microarray-results/id)
                jobs (make-batches 1000 :microarray-results eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :motif/id)
                jobs (make-batches 1000 :motif eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :oligo/id)
                jobs (make-batches 1000 :oligo eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :operon/id)
                jobs (make-batches 1000 :operon eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :paper/id)
                jobs (make-batches 1000 :paper eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :person/id)
                jobs (make-batches 1000 :person eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :pcr-product/id)
                jobs (make-batches 1000 :pcr-product eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :phenotype/id)
                jobs (make-batches 1000 :phenotype eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :position-matrix/id)
                jobs (make-batches 1000 :position-matrix eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :protein/id)
                jobs (make-batches 1000 :protein eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :pseudogene/id)
                jobs (make-batches 1000 :pseudogene eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :rearrangement/id)
                jobs (make-batches 1000 :rearrangement eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :rnai/id)
                jobs (make-batches 1000 :rnai eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :sequence/id)
                jobs (make-batches 1000 :sequence eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :strain/id)
                jobs (make-batches 1000 :strain eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :structure-data/id)
                jobs (make-batches 1000 :structure-data eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :transcript/id)
                jobs (make-batches 1000 :transcript eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :transgene/id)
                jobs (make-batches 1000 :transgene eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :transposon/id)
                jobs (make-batches 1000 :transposon eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :transposon-family/id)
                jobs (make-batches 1000 :transposon-family eids)]
            (doseq [job jobs]
              (>!! scheduler job)))
          (let [eids (get-eids-by-type db :wbprocess/id)
                jobs (make-batches 1000 :wbprocess eids)]
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

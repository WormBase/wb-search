(ns wb-es.bulk.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [>! <! >!! <!! go chan buffer close!]]
            [cheshire.core :as json]
            [datomic.api :as d]
            [durable-queue :as dq]
            [mount.core :as mount]
            [wb-es.datomic.data.core :refer [create-document]]
            [wb-es.datomic.data.gene :as gene]
            [wb-es.datomic.data.variation :as variation]
            [wb-es.datomic.db :refer [datomic-conn]]
            [wb-es.env :refer [es-base-url release-id]]
            [wb-es.mappings.core :refer [create-index]]
            [wb-es.web.setup :refer [es-connect]]
            [wb-es.snapshot.core :refer [connect-snapshot-repository save-snapshot get-next-snapshot-id]]))

(defn format-bulk
  "returns a new line delimited JSON based on
  an action name and a list of Documents (acoording to Document protocol)"
  ([action documents] (format-bulk action nil documents))
  ([action index documents]
   (->> documents
        (map (fn [doc]
               (let [action-data {action (if index
                                           (assoc (meta doc) :_index index) ; ie to specify test index
                                           (meta doc))}
                     action-name (name action)]
                 (cond
                   (or (= action-name "index")
                       (= action-name "create"))
                   (format "%s\n%s"
                           (json/generate-string action-data)
                           (json/generate-string doc))

                   (= action-name "update")
                   (if (:script doc)
                     (format "%s\n%s"
                             (json/generate-string action-data)
                             (json/generate-string doc))
                     (format "%s\n%s"
                             (json/generate-string action-data)
                             (json/generate-string {:doc doc
                                                    :doc_as_upsert true})
                             ))

                   (= action-name "delete")
                   (json/parse-string action-data))
                 )))
        (clojure.string/join "\n")
        (format "%s\n")))) ;trailing \n is necessary for Elasticsearch to parse the request

(defn submit
  "submit formatted new line delimited JSON to elasticsearch"
  [formatted-docs & {:keys [refresh index]}]
  (let [url-prefix (if index
                     (format "%s/%s" es-base-url index)
                     es-base-url)]
    (http/post (format "%s/_bulk?refresh=%s" url-prefix (or refresh "false"))
               {:headers {:content-type "application/x-ndjson"}
                :body formatted-docs})))

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
  ([batch-size order-info eids] (make-batches batch-size order-info "index" eids))
  ([batch-size order-info action eids]
   (->> eids
        (sort-by (fn [param]
                   (if (sequential? param)
                     (first param)
                     (identity param))))
        (partition batch-size batch-size [])
        (map (fn [batch]
               (with-meta batch {:order order-info
                                 :action action
                                 :size (count batch)
                                 :start (first batch)
                                 :end (last batch)}))))))

(defn run-index-batch
  "index data of a batch of datomic entity ids"
  [db index batch]
  (->> batch
       (map (fn [eid & other-params]
              (apply create-document (d/entity db eid) other-params)))
       (format-bulk (:action (meta batch)))
       ((fn [formatted-bulk]
          (submit formatted-bulk :index index)))
       )
  )

(def ^{:private true} q (dq/queues "/tmp/indexer-queue" {}))

(defn scheduler-put! [& args] (apply dq/put! q :indexing-jobs args))

(defn- scheduler-take! [& args] (apply dq/take! q :indexing-jobs args))

(defn- scheduler-complete! [& args] (apply dq/complete! args))

(defn- scheduler-retry! [& args] (apply dq/retry! args))


(defn run [& {:keys [db index-revision-number index-id skip-create-snapshot]
              :or {db (d/db datomic-conn)
                   index-revision-number 0
                   index-id (format "%s_v%s" release-id index-revision-number)}}]
  (let []
    (do
      (create-index index-id
                    :default-index (= index-revision-number 0)
                    :delete-existing true)
      (let [n-threads 4
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
                 (recur))
               (println "Done!")))))


        ;; multiple independent workers to execute jobs
        (dotimes [i n-threads]
          (go
            (loop []
              (if-let [job-ref (scheduler-take! 600000 nil)]
                ;; normal batches won't be nil
                ;; only get nil when channel is closed
                (let [job (deref job-ref)]
                  (do
                    (>! logger (or (meta job) :no_metadata))
                    (try
                      (run-index-batch db release-id job)
                      (scheduler-complete! job-ref)
                      (catch Exception e
                        ; retried items are added at the end of the queue
                        (scheduler-retry! job-ref)))
                    (recur)))
                (close! logger)))))

        (do
          ;; add jobs to scheduler in sequence

          (let [eids (get-eids-by-type db :gene/id)
                jobs (make-batches 100 :gene eids)]  ; smaller batch for slower ones
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :analysis/id)
                jobs (make-batches 1000 :analysis eids)]
            (doseq [job jobs]
               (scheduler-put! job)))
          (let [eids (get-eids-by-type db :anatomy-term/id)
                jobs (make-batches 1000 :anatomy-term eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :antibody/id)
                jobs (make-batches 1000 :antibody eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :cds/id)
                jobs (make-batches 1000 :cds eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :clone/id)
                jobs (make-batches 1000 :clone eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :construct/id)
                jobs (make-batches 1000 :construct eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :expression-cluster/id)
                jobs (make-batches 1000 :expression-cluster eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :expr-pattern/id)
                jobs (make-batches 1000 :expr-pattern eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :expr-profile/id)
                jobs (make-batches 1000 :expr-profile eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :do-term/id)
                jobs (make-batches 1000 :do-term eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :feature/id)
                jobs (make-batches 1000 :feature eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :gene-class/id)
                jobs (make-batches 1000 :gene-class eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :gene-cluster/id)
                jobs (make-batches 1000 :gene-cluster eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :go-term/id)
                jobs (make-batches 1000 :go-term eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :homology-group/id)
                jobs (make-batches 1000 :homology-group eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :interaction/id)
                jobs (make-batches 1000 :interaction eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :laboratory/id)
                jobs (make-batches 1000 :laboratory eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :life-stage/id)
                jobs (make-batches 1000 :life-stage eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :molecule/id)
                jobs (make-batches 1000 :molecule eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :microarray-results/id)
                jobs (make-batches 1000 :microarray-results eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :motif/id)
                jobs (make-batches 1000 :motif eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :oligo/id)
                jobs (make-batches 1000 :oligo eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :operon/id)
                jobs (make-batches 1000 :operon eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :paper/id)
                jobs (make-batches 1000 :paper eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :person/id)
                jobs (make-batches 1000 :person eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :pcr-product/id)
                jobs (make-batches 1000 :pcr-product eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :phenotype/id)
                jobs (make-batches 1000 :phenotype eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :picture/id)
                jobs (make-batches 1000 :picture eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :position-matrix/id)
                jobs (make-batches 1000 :position-matrix eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :protein/id)
                jobs (make-batches 1000 :protein eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :pseudogene/id)
                jobs (make-batches 1000 :pseudogene eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :rearrangement/id)
                jobs (make-batches 1000 :rearrangement eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :rnai/id)
                jobs (make-batches 1000 :rnai eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :sequence/id)
                jobs (make-batches 1000 :sequence eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :strain/id)
                jobs (make-batches 1000 :strain eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :structure-data/id)
                jobs (make-batches 1000 :structure-data eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :transcript/id)
                jobs (make-batches 1000 :transcript eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :transgene/id)
                jobs (make-batches 1000 :transgene eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :transposon/id)
                jobs (make-batches 1000 :transposon eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :transposon-family/id)
                jobs (make-batches 1000 :transposon-family eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :wbprocess/id)
                jobs (make-batches 1000 :wbprocess eids)]
            (doseq [job jobs]
              (scheduler-put! job)))
          (let [eids (get-eids-by-type db :variation/id)
                jobs (make-batches 100 :variation eids)] ; smaller batch for slower ones
            (doseq [job jobs]
              (scheduler-put! job)))

          )
        )

      (if-not skip-create-snapshot
        (let [repository-name "s3_repository"]
          (do
            (connect-snapshot-repository repository-name)
            (let [snapshot-id (get-next-snapshot-id repository-name release-id)]
              (save-snapshot index-id repository-name snapshot-id)))))
      )))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (es-connect)
    (println "Indexer starting!")
    (mount/start)
    (if-let [index-revision-number (first args)]
      (run :index-revision-number index-revision-number)
      (run))
    (mount/stop))
  )

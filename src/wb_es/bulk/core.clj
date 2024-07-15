(ns wb-es.bulk.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [>! <! >!! <!! go chan buffer close!]]
            [cheshire.core :as json]
            [datomic.api :as d]
            [durable-queue :as dq]
            [mount.core :as mount]
            [taoensso.timbre :refer [info debug warn error]]
            [wb-es.datomic.data.core :refer [create-document]]
            [wb-es.datomic.data.gene :as gene]
            [wb-es.datomic.data.variation :as variation]
            [wb-es.datomic.db :refer [datomic-conn]]
            [wb-es.env :refer [es-base-url release-id]]
            [wb-es.mappings.core :refer [create-index]]))

(defn format-bulk
  "returns a new line delimited JSON based on
  an action name and a list of Documents (according to Document protocol)"
  ([action documents] (format-bulk action nil documents))
  ([action index documents]
   (->> documents
        (map (fn [doc]
               (let [action-data {action (if index
                                           (assoc (meta doc) :_index index)
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
                                                    :doc_as_upsert true})))

                   (= action-name "delete")
                   (json/generate-string action-data)))))
        (clojure.string/join "\n")
        (format "%s\n"))))

(defn submit
  "submit formatted new line delimited JSON to elasticsearch"
  [formatted-docs & {:keys [refresh index]}]
  (let [url-prefix (if index
                     (format "%s/%s" es-base-url index)
                     es-base-url)]
    (let [response (http/post (format "%s/_bulk?refresh=%s" url-prefix (or refresh "false"))
                              {:headers {:content-type "application/x-ndjson"}
                               :body formatted-docs})]
      (->> (json/parse-string (:body response) true)
           (:items)
           (mapcat vals)
           (reduce (fn [result body]
                     (let [status (:status body)]
                       (cond
                         (< status 300) (update result :success inc)
                         :else (do
                                 (error body)
                                 (update result :error inc)))))
                   {:success 0 :error 0})))))

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
          (submit formatted-bulk :index index)))))

(def ^{:private true} q (dq/queues "/tmp/indexer-queue" {}))

(def ^{:private true} item-counts (atom 0))

(defn scheduler-put! [job & args]
  (do
    (swap! item-counts + (or (:size (meta job)) 0))
    (apply dq/put! q :indexing-jobs job args)))

(defn- scheduler-take! [& args] (apply dq/take! q :indexing-jobs args))

(defn- scheduler-complete! [& args] (apply dq/complete! args))

(defn- scheduler-retry! [& args] (apply dq/retry! args))

(defn- scheduler-stats []
  (->> {:expected-item-counts (deref item-counts)}
       (into (get (dq/stats q) "indexing_jobs"))))

(defn schedule-jobs-all [db]
  (doseq [entity-type [:genotype :gene :analysis :anatomy-term :antibody :cds
                       :clone :construct :expression-cluster :expr-pattern
                       :expr-profile :do-term :feature :gene-class :gene-cluster
                       :go-term :homology-group :interaction :laboratory :life-stage
                       :molecule :microarray-results :motif :oligo :operon :paper
                       :person :pcr-product :phenotype :picture :position-matrix
                       :protein :pseudogene :rearrangement :rnai :sequence :strain
                       :structure-data :transcript :transgene :transposon
                       :transposon-family :wbprocess :variation]]
    (let [eids (get-eids-by-type db (keyword (name entity-type) "id"))
          jobs (make-batches (if (= entity-type :gene) 100 1000) entity-type eids)]
      (doseq [job jobs]
        (scheduler-put! job)))))

(defn worker [db]
  (future
    (loop []
      (debug "Stats" (scheduler-stats))
      (if-let [job-ref (scheduler-take!)]
        (do
          (try
            (let [job (deref job-ref)
                  job-meta (meta job)]
              (try
                (debug "Starting" job-meta)
                (case (:action job-meta)
                  "snapshot" (let [index-id (:index job-meta)
                                   repository-name (:repository job-meta)
                                   snapshot-id "1"
                                   timeout 60000]
                               (debug (format "Snapshot paused for %s ms for jobs to finish..." timeout))
                               (Thread/sleep timeout)
                               (debug "Snapshot resumed")
                               (debug "Snapshot created" job-meta))
                  (let [job-report (run-index-batch db release-id job)]
                    (do
                      (debug "Indexed" (into job-meta job-report))
                      (if (> (:error job-report) 0)
                        (throw (Exception. "Batch contains failed items"))))))

                (scheduler-complete! job-ref)

                (catch Exception e
                  (error e)
                  (warn "Failed and scheduled to retry" job-meta)
                  (scheduler-retry! job-ref))))
            (catch java.io.IOException e
              (error "Corrupted reference" job-ref)
              (scheduler-complete! job-ref)))
          (recur))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [index-revision-number (or (first args) 0)
        index-id (format "%s_v%s" release-id index-revision-number)]
    (do
      (info "Indexer starting!")
      (mount/start)
      (create-index index-id
                    :default-index (= index-revision-number 0)
                    :delete-existing true)
      (let [db (d/db datomic-conn)]
        (do
          (schedule-jobs-all db)
          (scheduler-put! (with-meta {} {:action "snapshot"
                                         :index index-id}))
          (->> (partial worker db)
               (repeatedly 5)
               (pmap deref)
               (doall))

          (info "Stopping!" (scheduler-stats)))))))
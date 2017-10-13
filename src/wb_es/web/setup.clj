(ns wb-es.web.setup
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))



;; figure out which index to connect to

;; assert the index is available

(defn has-index
  [index-id]
  (try
    (http/get (format "%s/%s" es-base-url index-id))
    (catch Exception e
      (println (format "Index %s is not found locally." index-id)))))

(defn es-connect
  []
  (let [max-retry 50
        interval 1000
        counter (atom 0)
        connected (atom false)]
    (do
      (println (format "Trying to connect to %s" es-base-url))
      (while (not (deref connected))
        (do
          (swap! counter inc)
          (try
            (do
              (http/get es-base-url)
              (swap! connected not)
              (println "Connected."))
            (catch Exception e
              (if (>= (deref counter) max-retry)
                (throw (Exception. (format "Failed to connect to %s, terminated" es-base-url)))
                (do
                  (print ".")
                  (flush)
                  (Thread/sleep interval))))))))))

(defn connect-snapshot-repository
  ([repository-name]
   (connect-snapshot-repository repository-name
                                {"type" "s3"
                                 "settings" {"bucket" "wormbase-elasticsearch-snapshots"
                                             "region" "us-east-1"}}))
  ([repository-name repository-settings]
   (try
     (http/get (format "%s/_snapshot/%s" es-base-url repository-name)
               {:content-type "application/json"
                :body (json/generate-string repository-settings)})
     (catch Exception e
       (throw (Exception. "Cannot connect to elasticsearch snapshot repository"))))))

(defn get-snapshot-id
  [repository-name]
  (let [response (http/get (format "%s/_cat/snapshots/%s?format=json" es-base-url repository-name))
        all-snapshots (json/parse-string (:body response) true)
        id-pattern (re-pattern (format "snapshot_%s(_(\\d+))?" release-id))]
    (->> all-snapshots
         (filter (fn [snapshot]
                   (and
                    (= "SUCCESS" (:status snapshot))
                    (re-matches id-pattern (:id snapshot)))))
         (sort-by (fn [snapshot]
                    (let [[_ _ version-id] (re-matches id-pattern (:id snapshot))]
                      version-id)))
         (last)
         (:id))))

(defn restore-snapshot
  [repository-name snapshot-id])

;; (let [
;;       response
;;       (http/get (format "%s/%s" es-base-url index)
;;                 {:content-type "application/json"
;;                  :body (json/generate-string query)})]
;;   (json/parse-string (:body response) true))


;; if unavailable is provided:
;; attempt to restore it from snapshot

;; figure out which snapshot id to use
;;
;; wait until restoration finishes

(defn run
  "run setup"
  []
  (let [index-id release-id]
    (do
      (es-connect)
      (if (has-index index-id)
        (println (format "Elasticsearch index %s is found, starting server..." index-id))
        (let [repository-name "s3_repository"]
          (do
            (connect-snapshot-repository repository-name)
            (let [snapshot-id (get-snapshot-id repository-name)]
              (restore-snapshot repository-name snapshot-id)
              (println (format "Elasticsearch is restored from snapshot %s" snapshot-id))))))
      )))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (run))

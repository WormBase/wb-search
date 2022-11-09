(ns wb-es.snapshot.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))

; (defn connect-snapshot-repository
;   ([repository-name]
;    (connect-snapshot-repository repository-name
;                                 {"type" "s3"
;                                  "settings" {"bucket" "wormbase-elasticsearch-snapshots"
;                                              "region" "us-east-1"}}))
;   ([repository-name repository-settings]
;    (try
;      (http/put (format "%s/_snapshot/%s" es-base-url repository-name)
;                {:content-type "application/json"
;                 :body (json/generate-string repository-settings)})
;      (catch Exception e
;        (throw (Exception. "Cannot connect to elasticsearch snapshot repository"))))))

; (defn get-lateset-snapshot-id
;   [repository-name release-id & {:keys [partial?]
;                                  :or {partial? false}}]
;   (let [response (http/get (format "%s/_cat/snapshots/%s?format=json" es-base-url repository-name))
;         all-snapshots (json/parse-string (:body response) true)
;         id-pattern (re-pattern (format "snapshot_%s(_v(\\d+))?" release-id))]
;     (->> all-snapshots
;          (filter (fn [snapshot]
;                    (and
;                     (or partial?
;                         (= "SUCCESS" (:status snapshot)))
;                     (re-matches id-pattern (:id snapshot)))))
;          (sort-by (fn [snapshot]
;                     (let [[_ _ version-id] (re-matches id-pattern (:id snapshot))]
;                       (Integer. version-id))))
;          (last)
;          (:id))))

; (defn get-next-snapshot-id
;   [repository-name release-id]
;   (let [pattern (re-pattern (format "snapshot_%s(_v(\\d+))?" release-id))
;         current-id (get-lateset-snapshot-id repository-name release-id :partial? true)
;         next-version-id (if current-id
;                           (->> (re-matches pattern current-id)
;                                (last)
;                                (Integer.)
;                                (+ 1))
;                           0)]
;     (format "snapshot_%s_v%s" release-id next-version-id)))

(defn save-snapshot
  [index-id repository-name snapshot-id]
  (let [snapshot-url (format "%s/_snapshot/%s/%s" es-base-url repository-name snapshot-id)]
    (do
      (println (format "Saving index %s to %s in repository %s" index-id snapshot-id repository-name))
      (println (format "View progress with `curl -XGET %s`" snapshot-url))
      (http/put (format "%s?wait_for_completion=true" snapshot-url)
                {:headers {:content-type "application/json"}
                 :body (json/generate-string {:indices index-id})}
                )
      )))

; (defn restore-snapshot
;   [index-id repository-name snapshot-id]
;   (let [max-retry 50
;         interval 10000
;         counter (atom 0)
;         connected (atom false)]
;     (do
;       (try
;         (http/post (format "%s/_snapshot/%s/%s/_restore" es-base-url repository-name snapshot-id))
;         (catch Exception e
;           (throw (Exception. (format "Failed to restore snapshot %s" snapshot-id)))))
;       (println "Restoring index started")
;       (while (not (deref connected))
;         (do
;           (swap! counter inc)
;           (try
;             (do
;               (println (http/get (format "%s/%s/_search" es-base-url index-id)))
;               (swap! connected not)
;               (println "Connected."))
;             (catch Exception e
;               (if (>= (deref counter) max-retry)
;                 (throw (Exception. (format "Failed to connect to %s. Start up terminated." es-base-url)))
;                 (do
;                   (println (http/get (format "%s/_cat/recovery?format=json" es-base-url repository-name)))
;                   (flush)
;                   (Thread/sleep interval))))))))))

(ns wb-es.web.setup
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))



;; figure out which index to connect to

;; assert the index is available

(defn has-index
  [index-id]
  (let [response (http/get (format "%s/%s" es-base-url index-id))]
    (= 200 (:status response))))

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

(defn get-snapshot-id
  []
  (let [response (http/get (format "%s/_cat/snapshots/s3_repository?format=json" es-base-url))
        all-snapshots (json/parse-string (:body response) true)]
    (->> all-snapshots
         (filter (fn [snapshot]
                   (and
                    (= "SUCCESS" (:status snapshot))
                    (let [id-pattern (re-pattern (format "snapshot_%s(_\\d+)?" release-id))]
                      (re-matches id-pattern (:id snapshot))))))
         (first)
         (:id))))

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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [index-id release-id]
    (do
      (es-connect)
      (if (has-index index-id)
        (println (format "Index %s found, starting server..."))
        (do
          )))))

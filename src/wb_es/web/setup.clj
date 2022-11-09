(ns wb-es.web.setup
  (:gen-class)
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]
            ; [wb-es.snapshot.core :refer [connect-snapshot-repository
            ;                              get-lateset-snapshot-id
            ;                              restore-snapshot]]
            ))



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


(defn run
  "run setup"
  ; ([] (run release-id restore-from-snapshot))
  ([release-id]
     (let [index-id release-id]
       (do
         (es-connect)
;         (connect-snapshot-repository repository-name)
         (if (has-index index-id) println "Index should be set"
;           (println (format "Elasticsearch index %s is found locally. No attempt will be made to restore snapshots." index-id))
;           (if snapshot
;             (let [snapshot-id (if (= "latest" snapshot)
;                                 (get-lateset-snapshot-id repository-name release-id)
;                                 snapshot)]
;               (restore-snapshot index-id repository-name snapshot-id)
;               (println (format "Elasticsearch is restored from snapshot %s" snapshot-id))))
           )
         ))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (run))

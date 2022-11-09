(ns wb-es.env
  (:require [environ.core :refer [env]]))

(def datomic-uri (or (env :wb-db-uri) "datomic:ddb://us-east-1/WS261/wormbase"))

(def es-base-url
  (or (env :es-base-uri) "http://localhost:9200"))

(def release-id
  (->> (re-find #"WS\d+" datomic-uri)
       (clojure.string/lower-case)))

;(def restore-from-snapshot
;  (env :restore-from-snapshot))  ; options: latest, snapshot_id, nil

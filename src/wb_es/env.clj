(ns wb-es.env
  (:require [environ.core :refer [env]]))

(def datomic-uri (env :wb-db-uri))

(def elasticsearch-uri
  (or (env :elasticsearch-uri) "http://localhost:9200"))

(def release-id
  (re-find #"WS\d+" datomic-uri))

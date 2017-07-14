(ns wb-es.env
  (:require [environ.core :refer [env]]))

(def datomic-uri (env :wb-db-uri))

(def es-base-url
  (or (env :es-base-uri) "http://localhost:9200"))

(def release-id
  (->> (re-find #"WS\d+" datomic-uri)
       (clojure.string/lower-case)))

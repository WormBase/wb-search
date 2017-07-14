(ns wb-es.datomic.db
  (:require
   [datomic.api :as d]
   [mount.core :as mount]
   [wb-es.env :refer [datomic-uri]]))

(defn- connect []
  (d/connect datomic-uri))

(defn- disconnect [conn]
  (d/release conn))

(mount/defstate datomic-conn
  :start (connect)
  :stop (disconnect datomic-conn))

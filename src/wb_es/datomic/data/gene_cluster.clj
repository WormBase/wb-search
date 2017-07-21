;; title
;; definition
(ns wb-es.datomic.data.gene-cluster
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Gene-cluster [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:gene-cluster/id entity)
     :description (first (:gene-cluster/description entity))}))

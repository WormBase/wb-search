;; Description
;; species

(ns wb-es.datomic.data.expression-cluster
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Expression-cluster [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:expression-cluster/id entity)
     :description (first (:expression-cluster/description entity))
     :species (data-util/format-species-enum (:expression-cluster/species entity))}))

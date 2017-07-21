;; Gene_name
;; species

(ns wb-es.datomic.data.protein
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Protein [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:protein/id entity)
     :label (first (:protein/gene-name entity))
     :description (:protein/description entity)
     :species (data-util/format-species-enum (:protein/species entity))}))

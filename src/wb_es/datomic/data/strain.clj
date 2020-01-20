;; Genotype
;; species
(ns wb-es.datomic.data.strain
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Strain [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:strain/id entity)
     :label (or (:strain/public-name entity)
                (:strain/id entity))
     :description (:strain/genotype entity)
     :species (data-util/format-entity-species :strain/species entity)
     :remarks (->> entity
                   (:strain/remark)
                   (map :strain.remark/text))}))

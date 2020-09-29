;; Public_name
;; species

(ns wb-es.datomic.data.genotype
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Genotype [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:genotype/id entity)
     :label (:genotype/genotype-name entity)
     :other_names (:genotype/genotype-synonym entity)
     :species (data-util/format-entity-species :genotype/species entity)
     }))

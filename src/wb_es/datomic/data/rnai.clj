;;Gene
;; species
;; phenotype

(ns wb-es.datomic.data.rnai
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Rnai [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:rnai/id entity)
     :species (data-util/format-entity-species :rnai/species entity)
     :gene (->> (:rnai/gene entity)
                (map :rnai.gene/gene)
                (map data-util/pack-obj))
     :phenotype (->> (:rnai/phenotype entity)
                     (map :rnai.phenotype/phenotype)
                     (map data-util/pack-obj))
     :genotype (:rnai/genotype entity)
     :strain (data-util/pack-obj (:rnai/strain entity))}))

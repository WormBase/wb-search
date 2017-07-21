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
     :species (data-util/format-species-enum (:rnai/species entity))}))

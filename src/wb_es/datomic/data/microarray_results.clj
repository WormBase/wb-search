;; id as label
;; species
(ns wb-es.datomic.data.microarray-results
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Microarray-results [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:microarray-results/id entity)
     :label (:microarray-results/id entity)
     :species (data-util/format-species-enum (:microarray-results/species entity))}))

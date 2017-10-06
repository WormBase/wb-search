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
     :species (data-util/format-entity-species :microarray-results/species entity)
     ;; :gene (->> entity
     ;;            (:microarray-results/gene)
     ;;            (map :microarray-results.gene/gene)
     ;;            (map data-util/pack-obj))
     ;; :cds (->> entity
     ;;           (:microarray-results/cds)
     ;;           (map :microarray-results.cds/cds)
     ;;           (map data-util/pack-obj))
     }))

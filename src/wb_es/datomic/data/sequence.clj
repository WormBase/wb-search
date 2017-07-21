;; id
;; species
(ns wb-es.datomic.data.sequence
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Sequence [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:sequence/id entity)
     :species (data-util/format-species-enum (:sequence/species entity))}))

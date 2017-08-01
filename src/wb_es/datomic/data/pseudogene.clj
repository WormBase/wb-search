;; Brief_identification
;; species

(ns wb-es.datomic.data.pseudogene
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Pseudogene [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:pseudogene/id entity)
     :description (->> entity
                       (:pseudogene/brief-identification)
                       (:pseudogene.brief-identification/text))
     :species (data-util/format-entity-species :pseudogene/species entity)}))

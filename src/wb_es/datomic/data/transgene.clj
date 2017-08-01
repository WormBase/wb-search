;; Public_name
;; summary
;; Synonym

(ns wb-es.datomic.data.transgene
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Transgene [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:transgene/id entity)
     :label (:transgene/public-name entity)
     :other_names (:transgene/synonym entity)
     :description (->> entity
                       (:transgene/summary)
                       (:transgene.summary/text))
     :species (data-util/format-entity-species :transgene/species entity)}))

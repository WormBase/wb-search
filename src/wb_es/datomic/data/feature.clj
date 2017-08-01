;; Description
;; species
(ns wb-es.datomic.data.feature
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Feature [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:feature/id entity)
     :label (:feature/id entity)
     :description (first (:feature/description entity))
     :species (data-util/format-entity-species :feature/species entity)}))

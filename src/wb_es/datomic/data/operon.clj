;; Description
;; species

(ns wb-es.datomic.data.operon
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Operon [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:operon/id entity)
     :description (->> entity
                       (:operon/description)
                       (:operon.description/text))
     :species (data-util/format-species-enum (:operon/species entity))}))

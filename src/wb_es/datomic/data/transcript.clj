;; id
;; species

(ns wb-es.datomic.data.transcript
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Transcript [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:transcript/id entity)
     :label (:transcript/id entity)
     :description (:transcript/brief-identification entity)
     :remarks (->> (:transcript/db-remark entity)
                   (map :transcript.db-remark/text))
     :method (->> (:locatable/method entity)
                  (:method/id))
     :species (data-util/format-entity-species :transcript/species entity)}))

;; Concise_description, Provisional_description, Other_description
;; various kinds of names
(ns wb-es.datomic.data.gene
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Gene [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:gene/id entity)
     :label (:gene/public-name entity)
     :other_names (->> (:gene/other-name entity)
                       (map :gene.other-name/text)
                       (cons (:gene/sequence-name entity)))
     :description (or
                   (->> entity
                        (:gene/concise-description)
                        (first)
                        (:gene.concise-description/text))
                   (->> entity
                        (:gene/automated-description)
                        (first)
                        (:gene.automated-description/text)))
     :species (let [species-entity (:gene/species entity)]
                (if-let [[_ bioproject-id] (re-matches #"(PRJ.*):.*" (:gene/id entity))]
                  (data-util/format-species-enum-memoized species-entity bioproject-id)
                  (data-util/format-species-enum-memoized species-entity)))
     :species_name (let [species-entity (:gene/species entity)]
                     (if-let [[_ bioproject-id] (re-matches #"(PRJ.*):.*" (:gene/id entity))]
                       (data-util/format-species-text-memoized species-entity bioproject-id)
                       (data-util/format-species-text-memoized species-entity)))}))

;; Primary_name
;; Description

(ns wb-es.datomic.data.phenotype
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Phenotype [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:phenotype/id entity)
     :label (->> entity
                 (:phenotype/primary-name)
                 (:phenotype.primary-name/text))
     :other_names (->> (:phenotype/synonym entity)
                       (map :phenotype.synonym/text))
     :description (->> entity
                       (:phenotype/description)
                       (:phenotype.description/text))}))

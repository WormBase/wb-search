;; species

(ns wb-es.datomic.data.clone
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Clone [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:clone/id entity)
     :species (data-util/format-species-enum (:clone/species entity))}))

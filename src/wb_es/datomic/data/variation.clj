;; Public_name
;; species

(ns wb-es.datomic.data.variation
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Variation [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:variation/id entity)
     :label (:variation/public-name entity)
     :species (data-util/format-entity-species :variation/species entity)
     }))

(deftype Gene [gene]
  data-util/Document
  (metadata [this] (data-util/default-metadata gene))
  (data [this]
    (let [packed-gene (data-util/pack-obj gene)]
      {:script
       {:inline "ctx._source.gene =  ctx._source.containsKey(\"gene\") ? ctx._source.gene + gene : [gene]"
        :params {:gene packed-gene}}
       :upsert {:gene [packed-gene]}})))

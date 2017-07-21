;; Remark
;; species
(ns wb-es.datomic.data.transposon
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Transposon [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:transposon/id entity)
     :wbid_as_label (:transposon/id entity)
     :species (data-util/format-species-enum (:transposon/species entity))}))

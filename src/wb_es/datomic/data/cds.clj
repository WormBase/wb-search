;;
;; brief-identification? (very sparce)
;; remark?
;; corresponding protein?
(ns wb-es.datomic.data.cds
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Cds [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:cds/id entity)
     :remarks (->> (:cds/db-remark entity)
                   (map :cds.db-remark/text))
     :species (data-util/format-entity-species :cds/species entity)}))

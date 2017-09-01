;; id as label

(ns wb-es.datomic.data.pcr-product
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Pcr-product [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:page_type "pcr_oligo"
     :wbid (:pcr-product/id entity)
     :label (:pcr-product/id entity)}))

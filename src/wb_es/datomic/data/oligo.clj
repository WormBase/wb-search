;; id as label

(ns wb-es.datomic.data.oligo
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Oligo [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:page_type "pcr-oligo"
     :wbid (:oligo/id entity)
     :label (:oligo/id entity)}))

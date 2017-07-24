;; id as label

(ns wb-es.datomic.data.oligo
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Oligo [entity]
  data-util/Document
  (metadata [this] (assoc (data-util/default-metadata entity) :_type "pcr-oligo"))
  (data [this]
    {:wbid (:oligo/id entity)
     :label (:oligo/id entity)}))

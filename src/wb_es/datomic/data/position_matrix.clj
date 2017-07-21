;; description


(ns wb-es.datomic.data.position-matrix
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Position-matrix [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:position-matrix/id entity)
     :description (->> entity
                       (:position-matrix/description)
                       (first)
                       (:position-matrix.description/text))}))

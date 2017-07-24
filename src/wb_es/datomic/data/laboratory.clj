;; id as label for autocompletion
(ns wb-es.datomic.data.laboratory
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Laboratory [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:laboratory/id entity)
     :label (:laboratory/id entity)}))

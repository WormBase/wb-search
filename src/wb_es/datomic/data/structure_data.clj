;; id
(ns wb-es.datomic.data.structure-data
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Structure-data [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:structure-data/id entity)}))

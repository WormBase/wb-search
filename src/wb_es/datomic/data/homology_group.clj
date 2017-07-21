;; Remark
(ns wb-es.datomic.data.homology-group
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Homology-group [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:homology-group/id entity)}))

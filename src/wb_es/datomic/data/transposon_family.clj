;; id
(ns wb-es.datomic.data.transposon-family
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Transposon-family [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:transposon-family/id entity)}))

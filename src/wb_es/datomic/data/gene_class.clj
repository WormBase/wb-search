;; Description

(ns wb-es.datomic.data.gene-class
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Gene-class [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:gene-class/id entity)
     :label (:gene-class/id entity)
     :description (:gene-class/description entity)}))

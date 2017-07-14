(ns wb-es.datomic.data.go-term
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Go-term [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:go-term/id entity)
     :label (:go-term/definition entity)}))



(defn metadata [go-term-entity]
  (let []
    {:_index 1
     :_type 1
     :_id 1}))

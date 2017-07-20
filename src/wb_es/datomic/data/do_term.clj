;; name
;; definition

(ns wb-es.datomic.data.do-term
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Do-term [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:do-term/id entity)
     :label (:do-term/name entity)
     :description (:do-term/definition entity)}))

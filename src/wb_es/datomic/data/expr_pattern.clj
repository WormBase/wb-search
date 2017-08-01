;; Pattern
(ns wb-es.datomic.data.expr-pattern
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Expr-pattern [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:expr-pattern/id entity)
     :description (first (:expr-pattern/pattern entity))
     :species (data-util/format-entity-species :expr-pattern/species entity)}))

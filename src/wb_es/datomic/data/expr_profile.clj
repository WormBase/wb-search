;; Pattern

(ns wb-es.datomic.data.expr-profile
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Expr-profile [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:expr-profile/id entity)}))

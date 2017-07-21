;; public-name
;; other-name
;; Definition
(ns wb-es.datomic.data.life-stage
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Life-stage [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:life-stage/id entity)
     :label (:life-stage/public-name entity)
     :other_names (:life-stage/other-name entity)
     :description (first (:life-stage/definition entity))}))

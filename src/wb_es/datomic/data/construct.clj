;; interesting Summary

(ns wb-es.datomic.data.construct
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Construct [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:construct/id entity)
     :label (->> entity
                 (:construct/summary)
                 (:construct.summary/text))}))

;; id as label
(ns wb-es.datomic.data.motif
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Motif [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:motif/id entity)
     :wbid_as_label (if-not (seq (:motif/title entity))
                      (:motif/id entity))
     :label (first (:motif/title entity) )}))

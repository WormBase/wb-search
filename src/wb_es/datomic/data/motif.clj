;; id as label
(ns wb-es.datomic.data.motif
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Motif [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:motif/id entity)
     :label (if-let [title (first (:motif/title entity))]
              title
              (:motif/id entity))}))

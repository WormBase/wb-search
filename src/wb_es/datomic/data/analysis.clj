;; :analysis/title
;; :analysis/description
(ns wb-es.datomic.data.analysis
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Analysis [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:analysis/id entity)
     :label (first (:analysis/title entity))
     :description (first (:analysis/description entity))}))

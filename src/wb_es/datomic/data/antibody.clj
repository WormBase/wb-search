;; antibody/id (meaning full id)
;; antibody/summary
(ns wb-es.datomic.data.antibody
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Antibody [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:antibody/id entity)
     :label (:antibody/id entity)
     :other_names (:antibody/other-name entity)
     :description (->> entity
                       (:antibody/summary)
                       (:antibody.summary/text))}))

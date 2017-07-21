;; anatomy-term/term
;; anatomy-term/definition
(ns wb-es.datomic.data.anatomy-term
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Anatomy-term [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:anatomy-term/id entity)
     :label (->> entity
                 (:anatomy-term/term)
                 (:anatomy-term.term/text))
     :description (->> entity
                       (:anatomy-term/definition)
                       (:anatomy-term.definition/text))}))

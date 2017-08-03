;; Description
;; species

(ns wb-es.datomic.data.expression-cluster
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Expression-cluster [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:expression-cluster/id entity)
     :description (if-let [algorithms (:expression-cluster/algorithm entity)]
                    (format "%s\n%s"
                            (clojure.string/join ", " algorithms)
                            (first (:expression-cluster/description entity)))
                    (first (:expression-cluster/description entity)))
     :species (data-util/format-entity-species :expression-cluster/species entity)}))

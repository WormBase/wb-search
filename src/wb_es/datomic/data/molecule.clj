;; Public_name
;; IUPAC 4-methyl-1,3-thiazole
(ns wb-es.datomic.data.molecule
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Molecule [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:molecule/id entity)
     :label (first (:molecule/public-name entity))
     :other_names (:molecule/iupac entity)}))

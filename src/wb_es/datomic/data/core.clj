(ns wb-es.datomic.data.core
  (:require [wb-es.datomic.data.do-term :as do-term]
            [wb-es.datomic.data.feature :as feature]
            [wb-es.datomic.data.gene :as gene]
            [wb-es.datomic.data.gene-class :as gene-class]
            [wb-es.datomic.data.go-term :as go-term]
            [wb-es.datomic.data.interaction :as interaction]
            [wb-es.datomic.data.molecule :as molecule]
            [wb-es.datomic.data.paper :as paper]
            [wb-es.datomic.data.phenotype :as phenotype]
            [wb-es.datomic.data.strain :as strain]
            [wb-es.datomic.data.variation :as variation]
            [wb-es.datomic.data.util :as data-util]))

(defn create-document
  "returns document of the desirable type"
  [entity]
  (let [constructor-function
        (case (data-util/get-ident-attr entity)
          :do-term/id do-term/->Do-term
          :feature/id feature/->Feature
          :gene/id gene/->Gene
          :gene-class/id gene-class/->Gene-class
          :go-term/id go-term/->Go-term
          :interaction/id interaction/->Interaction
          :molecule/id molecule/->Molecule
          :paper/id paper/->Paper
          :phenotype/id phenotype/->Phenotype
          :strain/id strain/->Strain
          :variation/id variation/->Variation
          (throw (Exception. "Not sure how to handle the data type. Throw an error to let you know")))]
    (constructor-function entity)))

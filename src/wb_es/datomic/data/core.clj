(ns wb-es.datomic.data.core
  (:require [wb-es.datomic.data.do-term :as do-term]
            [wb-es.datomic.data.gene :as gene]
            [wb-es.datomic.data.go-term :as go-term]
            [wb-es.datomic.data.paper :as paper]
            [wb-es.datomic.data.variation :as variation]
            [wb-es.datomic.data.util :as data-util]))

(defn create-document
  "returns document of the desirable type"
  [entity]
  (let [constructor-function
        (case (data-util/get-ident-attr entity)
          :do-term/id do-term/->Do-term
          :gene/id gene/->Gene
          :go-term/id go-term/->Go-term
          :paper/id paper/->Paper
          :variation/id variation/->Variation
          (throw (Exception. "Not sure how to handle the data type. Throw an error to let you know")))]
    (constructor-function entity)))

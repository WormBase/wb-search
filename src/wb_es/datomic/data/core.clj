(ns wb-es.datomic.data.core
  (:require [wb-es.datomic.data.go-term :as go-term]
            [wb-es.datomic.data.util :as data-util]))

(defn create-document
  "returns document of the desirable type"
  [entity]
  (let [constructor-function
        (case (data-util/get-ident-attr entity)
          :go-term/id go-term/->Go-term)]
    (constructor-function entity)))

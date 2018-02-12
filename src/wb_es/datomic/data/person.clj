;; Full_name
;; Also_known_as

(ns wb-es.datomic.data.person
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Person [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:person/id entity)
     :label (:person/standard-name entity)
     :other_names (concat [(:person/full-name entity)]
                          (:person/also-known-as entity)
                          (->> (:person/possibly-publishes-as entity)
                               (map :author/id)))
     :institution (->> (:person/address entity)
                       (:address/institution)
                       (clojure.string/join "\n"))}))

(ns wb-es.datomic.data.paper
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Paper [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:paper/id entity)
     :label (:paper/brief-citation entity)
     :description (->> (:paper/abstract entity)
                       (map :longtext/text)
                       (clojure.string/join "\n"))
     :paper_type (->> entity
                      (:paper/type)
                      (map :paper.type/type)
                      (map data-util/format-enum))}))

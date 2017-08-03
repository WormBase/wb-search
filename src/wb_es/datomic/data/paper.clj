(ns wb-es.datomic.data.paper
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(defn pack-author [author-holder]
  (or
   (->> (:affiliation/person author-holder)
        (first)
        (data-util/pack-obj))
   (-> (:paper.author/author author-holder)
       (data-util/pack-obj))))

(deftype Paper [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:paper/id entity)
     :label (:paper/brief-citation entity)
     :description (->> (:paper/abstract entity)
                       (map :longtext/text)
                       (clojure.string/join "\n"))
     :author (map pack-author (:paper/author entity))
     :paper_type (->> entity
                      (:paper/type)
                      (map :paper.type/type)
                      (map data-util/format-enum))
     :journal (:paper/journal entity)
     :year (some-> (:paper/publication-date entity)
                   (clojure.string/split #"-")
                   (first))}))

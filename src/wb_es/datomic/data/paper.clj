(ns wb-es.datomic.data.paper
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(defn pack-author [author-holder]
  (let [author (:paper.author/author author-holder)
        person (first (:affiliation/person author-holder))]
    (if person
      (-> (data-util/pack-obj person)
          (assoc :label (:author/id author)))
      (data-util/pack-obj author))))

(deftype Paper [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:paper/id entity)
     :other_unique_ids (->> (:paper/name entity)
                            (map (fn [id]
                                   (if-let [match (re-matches #"^(?:doi[^/]*)?(10\.[^/]+.+)$" id)]
                                     (second match)
                                     id))))
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

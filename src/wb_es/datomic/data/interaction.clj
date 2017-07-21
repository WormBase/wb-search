;; Interaction_summary
;; has to construct interaction label from interactors for auto completion

(ns wb-es.datomic.data.interaction
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(defn get-label [entity]
  (->> (concat (->> entity
                    (:interaction/interactor-overlapping-gene)
                    (map (comp :gene/public-name
                               :interaction.interactor-overlapping-gene/gene)))
               (->> entity
                    (:interaction/feature-interactor)
                    (map (comp :feature/public-name
                               :interaction.feature-interactor/feature)))
               (->> entity
                    (:interaction/other-interactor)
                    (map :interaction.other-interactor/text))
               (->> entity
                    (:interaction/molecule-interactor)
                    (map (comp first
                               :molecule/public-name
                               :interaction.molecule-interactor/molecule))))
       (sort)
       (clojure.string/join " : ")))

(deftype Interaction [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:interaction/id entity)
     :label (get-label entity)
     :description (->> (:interaction/interaction-summary entity)
                       (first)
                       (:interaction.interaction-summary/text))}))

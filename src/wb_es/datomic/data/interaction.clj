;; Interaction_summary
;; has to construct interaction label from interactors for auto completion

(ns wb-es.datomic.data.interaction
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(defn get-label [entity]
  (->> (concat (->> entity
                    (:interaction/interactor-overlapping-gene)
                    (map :interaction.interactor-overlapping-gene/gene)
                    (map (partial data-util/obj-label "gene")))
               (->> entity
                    (:interaction/feature-interactor)
                    (map :interaction.feature-interactor/feature)
                    (map (partial data-util/obj-label "feature")))
               (->> entity
                    (:interaction/other-interactor)
                    (map :interaction.other-interactor/text))
               (->> entity
                    (:interaction/molecule-interactor)
                    (map :interaction.molecule-interactor/molecule)
                    (map (partial data-util/obj-label "molecule")))
               (->> entity
                    (:interaction/pcr-interactor)
                    (map :interaction.pcr-interactor/pcr-product)
                    (map (partial data-util/obj-label "pcr-product")))
               (->> entity
                    (:interaction/sequence-interactor)
                    (map :interaction.sequence-interactor/sequence)
                    (map (partial data-util/obj-label "sequence")))
               (->> entity
                    (:interaction/variation-interactor)
                    (map :interaction.variation-interactor/variation)
                    (map (partial data-util/obj-label "variation")))
               (->> entity
                    (:interaction/rearrangement)
                    (map :interaction.rearrangement/rearrangement)
                    (map (partial data-util/obj-label "rearrangement")))

               )
       (sort)
       (#(case (count %)
           0 nil
           1 (format "Interaction involving %s" (first %))
           (clojure.string/join " : " %)))))

(deftype Interaction [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:interaction/id entity)
     :label (get-label entity)
     :description (->> (:interaction/interaction-summary entity)
                       (first)
                       (:interaction.interaction-summary/text))}))

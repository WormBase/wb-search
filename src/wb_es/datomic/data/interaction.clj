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
               ;; (->> entity
               ;;      (:interaction/variation-interactor)
               ;;      (map :interaction.variation-interactor/variation)
               ;;      (map (partial data-util/obj-label "variation")))
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

(defn interaction-group-id [entity]
  (let [prefix (->> entity
                    (:interaction/interactor-overlapping-gene)
                    (map :interaction.interactor-overlapping-gene/gene)
                    (map :db/id)
                    (sort)
                    (map str))]
    (if (>= (count prefix) 2)
      (clojure.string/join ":" prefix) ; ignore suffix in this case
      (let [suffix (->> (concat (->> entity
                                     (:interaction/interactor-overlapping-gene)
                                     (map :interaction.interactor-overlapping-gene/gene)
                                     (map :db/id))
                                (->> entity
                                     (:interaction/feature-interactor)
                                     (map :interaction.feature-interactor/feature)
                                     (map :db/id))
                                (->> entity
                                     (:interaction/other-interactor)
                                     (map :interaction.other-interactor/text))
                                (->> entity
                                     (:interaction/molecule-interactor)
                                     (map :interaction.molecule-interactor/molecule)
                                     (map :db/id))
                                (->> entity
                                     (:interaction/pcr-interactor)
                                     (map :interaction.pcr-interactor/pcr-product)
                                     (map :db/id))
                                (->> entity
                                     (:interaction/sequence-interactor)
                                     (map :interaction.sequence-interactor/sequence)
                                     (map :db/id))
                                ;; (->> entity
                                ;;      (:interaction/variation-interactor)
                                ;;      (map :interaction.variation-interactor/variation)
                                ;;      (map :db/id))
                                (->> entity
                                     (:interaction/rearrangement)
                                     (map :interaction.rearrangement/rearrangement)
                                     (map :db/id))
                                )
                        (map str)
                        (sort))]
        (->> (concat prefix suffix)
             (clojure.string/join ":"))))))

(defn get-interaction-type [entity]
  (let [types (:interaction/type entity)]
    (reduce (fn [result type]
              (let [[type-category subtype]
                    (-> (name type)
                        (clojure.string/split #":"))]
                (assoc result (format "interaction_type_%s" type-category) subtype)))
            {} types)))

(deftype Interaction [entity]
  data-util/Document
  (metadata [this] (assoc (data-util/default-metadata entity)
                     :_routing (interaction-group-id entity)))
  (data [this]
    (merge
     (get-interaction-type entity)
     {:wbid (:interaction/id entity)
                                        ;     :label (get-label entity)
      :description (->> (:interaction/interaction-summary entity)
                        (first)
                        (:interaction.interaction-summary/text))
      :throughput (if-let [throughput-raw (->> entity
                                               (:interaction/throughput))]
                    (-> throughput-raw
                        (str)
                        (clojure.string/split #"/")
                        (last)))
      :detection_method (if-let [method-raw (->> entity
                                                 (:interaction/detection-method)
                                                 (first)
                                                 (:interaction.detection-method/value))]
                          (-> method-raw
                              (str)
                              (clojure.string/split #"/")
                              (last)))
      :join {:name "interaction"
             :parent (interaction-group-id entity)}
      })))

(defn get-shared-slims [entity]
  (let [db (d/entity-db entity)
        genes (->> entity
                   (:interaction/interactor-overlapping-gene)
                   (map :interaction.interactor-overlapping-gene/gene)
                   (map :db/id))]
    (if (= (count genes) 2)
      (let [slims-by-aspect (->> genes
                                 (map #(data-util/get-slims-for-gene db %))
                                 (map set)
                                 (apply clojure.set/intersection)
                                 (data-util/group-slims-by-aspect db))]
        (reduce (fn [result [aspect slims]]
                  (let [key (format "count_%s" (name aspect))]
                    (assoc result key (count slims))))
                slims-by-aspect
                slims-by-aspect)))))

(deftype Interaction-group [entity]
  data-util/Document
  (metadata [this] (assoc (data-util/default-metadata entity)
                     :_id (interaction-group-id entity)))
  (data [this]
    (assoc (get-shared-slims entity)
      :page_type "interaction_group"
      :label (get-label entity)
      :join {:name "interaction_group"})))

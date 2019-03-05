;; Concise_description, Provisional_description, Other_description
;; various kinds of names
(ns wb-es.datomic.data.gene
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(def slims
  (memoize (fn [db]
             (->>
              ["GO:0003824" ;molecular function
               ;; "GO:0004872"
               "GO:0005102"
               "GO:0005215"
               "GO:0005198"
               "GO:0008092"
               "GO:0003677"
               "GO:0003723"
               ;; "GO:0001071"
               "GO:0036094"
               "GO:0046872"
               "GO:0030246"
               ;; "GO:0003674"
               "GO:0008283" ;biological process
               "GO:0071840"
               "GO:0051179"
               "GO:0032502"
               "GO:0000003"
               "GO:0002376"
               "GO:0050877"
               "GO:0050896"
               "GO:0023052"
               "GO:0010467"
               "GO:0019538"
               "GO:0006259"
               "GO:0044281"
               "GO:0050789"
               "GO:0042592"
               "GO:0007610"
               ;; "GO:0008150"
               "GO:0005576" ;cellular component
               "GO:0005737"
               "GO:0005856"
               "GO:0005739"
               "GO:0005634"
               "GO:0005694"
               "GO:0016020"
               "GO:0031982"
               "GO:0071944"
               "GO:0030054"
               "GO:0042995"
               "GO:0032991"
               "GO:0045202"
               ;; "GO:0005575"
               ]
              (map vector (repeat :go-term/id))
              (map #(d/entid db %))))))

(def aspects
  (memoize (fn [db]
             (->> ["GO:0008150" "GO:0003674" "GO:0005575"]
                  (map vector (repeat :go-term/id))
                  (map #(d/entid db %))))))

(defn get-slims-for-gene [entity]
  (let [db (d/entity-db entity)]
    (d/q '[:find [?slim ...]
           :in $ ?gene [?slim ...]
           :where
           [?anno :go-annotation/gene ?gene]
           (or-join [?anno ?slim]
                    [?anno :go-annotation/go-term ?slim]
                    (and
                     [?anno :go-annotation/go-term ?term]
                     [?term :go-term/ancestor ?slim]))]
         db
         (:db/id entity)
         (slims db))))

(def process-slim
  (memoize
   (fn [db slim]
     (let [term-entity (d/entity db slim)
           aspect-name (d/q '[:find ?aspect-name .
                              :in $ ?slim [?aspect ...]
                              :where
                              [?slim :go-term/ancestor ?aspect]
                              [?aspect :go-term/name ?aspect-name]]
                            db slim (aspects db))]
       {:aspect (keyword aspect-name)
        :label (first (:go-term/name term-entity))}))))

(defn go-slim-terms [entity]
  (let [db (d/entity-db entity)
        gene-slims (get-slims-for-gene entity)]
    (reduce (fn [result slim]
              (let [processed-slim (process-slim db slim)]
                (update result (:aspect processed-slim) conj (:label processed-slim))))
            {}
            gene-slims)))

(deftype Gene [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    (assoc (go-slim-terms entity)
      :wbid (:gene/id entity)
      :label (:gene/public-name entity)
      :other_names (->> (concat (->> (:gene/other-name entity)
                                     (map :gene.other-name/text))
                                (:gene/molecular-name entity))
                        (cons (:gene/sequence-name entity)))
      :description (->> entity
                        (:gene/automated-description)
                        (first)
                        (:gene.automated-description/text))
      :legacy_description (->> entity
                               (:gene/concise-description)
                               (first)
                               (:gene.concise-description/text))
      :species (data-util/format-entity-species :gene/species entity)
      :allele (->> entity
                     (:variation.gene/_gene)
                     (map :variation/_gene)
                     (filter :variation/allele)
                     (map data-util/pack-obj))
      :dead (= "dead"
                 (some->> (:gene/status entity)
                          (:gene.status/status)
                          (name)))
      :merged_into (->> entity
                          (:gene/version-change)
                          (map :gene-history-action/merged-into)
                          (filter identity)
                          (first)
                          (data-util/pack-obj)))))

;; (deftype Variation [variation]
;;   data-util/Document
;;   (metadata [this] (data-util/default-metadata variation))
;;   (data [this]
;;     (let [packed-variation (data-util/pack-obj variation)]
;;       {:script
;;        {:inline "ctx._source.allele = ctx._source.containsKey(\"allele\") ? + ctx._source.allele + allele : [allele]"
;;         :params {:allele packed-variation}}
;;        :upsert {:allele [packed-variation]}})))

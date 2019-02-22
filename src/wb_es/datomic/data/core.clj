(ns wb-es.datomic.data.core
  (:require [wb-es.datomic.data.analysis :as analysis]
            [wb-es.datomic.data.anatomy-term :as anatomy-term]
            [wb-es.datomic.data.antibody :as antibody]
            [wb-es.datomic.data.cds :as cds]
            [wb-es.datomic.data.clone :as clone]
            [wb-es.datomic.data.construct :as construct]
            [wb-es.datomic.data.do-term :as do-term]
            [wb-es.datomic.data.expression-cluster :as expression-cluster]
            [wb-es.datomic.data.expr-pattern :as expr-pattern]
            [wb-es.datomic.data.expr-profile :as expr-profile]
            [wb-es.datomic.data.feature :as feature]
            [wb-es.datomic.data.gene :as gene]
            [wb-es.datomic.data.gene-class :as gene-class]
            [wb-es.datomic.data.gene-cluster :as gene-cluster]
            [wb-es.datomic.data.go-term :as go-term]
            [wb-es.datomic.data.homology-group :as homology-group]
            [wb-es.datomic.data.interaction :as interaction]
            [wb-es.datomic.data.laboratory :as laboratory]
            [wb-es.datomic.data.life-stage :as life-stage]
            [wb-es.datomic.data.molecule :as molecule]
            [wb-es.datomic.data.microarray-results :as microarray-results]
            [wb-es.datomic.data.motif :as motif]
            [wb-es.datomic.data.oligo :as oligo]
            [wb-es.datomic.data.operon :as operon]
            [wb-es.datomic.data.paper :as paper]
            [wb-es.datomic.data.person :as person]
            [wb-es.datomic.data.pcr-product :as pcr-product]
            [wb-es.datomic.data.phenotype :as phenotype]
            [wb-es.datomic.data.picture :as picture]
            [wb-es.datomic.data.position-matrix :as position-matrix]
            [wb-es.datomic.data.protein :as protein]
            [wb-es.datomic.data.pseudogene :as pseudogene]
            [wb-es.datomic.data.rearrangement :as rearrangement]
            [wb-es.datomic.data.rnai :as rnai]
            [wb-es.datomic.data.sequence :as sequence]
            [wb-es.datomic.data.strain :as strain]
            [wb-es.datomic.data.structure-data :as structure-data]
            [wb-es.datomic.data.transcript :as transcript]
            [wb-es.datomic.data.transgene :as transgene]
            [wb-es.datomic.data.transposon :as transposon]
            [wb-es.datomic.data.transposon-family :as transposon-family]
            [wb-es.datomic.data.variation :as variation]
            [wb-es.datomic.data.wbprocess :as wbprocess]
            [wb-es.datomic.data.util :as data-util]))

(defn create-document
  "returns document of the desirable type"
  ([entity] (let [scope (->> (data-util/get-ident-attr entity)
                             (namespace)
                             (keyword))]
              (create-document entity scope)))
  ([scope entity]
   (let [constructor-function
         (case scope
           :analysis analysis/->Analysis
           :anatomy-term anatomy-term/->Anatomy-term
           :antibody antibody/->Antibody
           :cds cds/->Cds
           :clone clone/->Clone
           :construct construct/->Construct
           :do-term do-term/->Do-term
           :expression-cluster expression-cluster/->Expression-cluster
           :expr-pattern expr-pattern/->Expr-pattern
           :expr-profile expr-profile/->Expr-profile
           :feature feature/->Feature
           :gene gene/->Gene
           :gene-class gene-class/->Gene-class
           :go-term go-term/->Go-term
           :gene-cluster gene-cluster/->Gene-cluster
           :homology-group homology-group/->Homology-group
           :interaction interaction/->Interaction
           :laboratory laboratory/->Laboratory
           :life-stage life-stage/->Life-stage
           :molecule molecule/->Molecule
           :microarray-results microarray-results/->Microarray-results
           :motif motif/->Motif
           :oligo oligo/->Oligo
           :operon operon/->Operon
           :paper paper/->Paper
           :person person/->Person
           :pcr-product pcr-product/->Pcr-product
           :phenotype phenotype/->Phenotype
           :picture picture/->Picture
           :position-matrix position-matrix/->Position-matrix
           :protein protein/->Protein
           :pseudogene pseudogene/->Pseudogene
           :rearrangement rearrangement/->Rearrangement
           :rnai rnai/->Rnai
           :sequence sequence/->Sequence
           :strain strain/->Strain
           :structure-data structure-data/->Structure-data
           :transcript transcript/->Transcript
           :transgene transgene/->Transgene
           :transposon transposon/->Transposon
           :transposon-family transposon-family/->Transposon-family
           :variation variation/->Variation
           :wbprocess wbprocess/->Wbprocess
           (throw (Exception. "Not sure how to handle the data type. Throw an error to let you know")))]
     (let [document (constructor-function entity)
           doc-data (.data document)
           doc-meta (.metadata document)
           page-type (clojure.string/replace (data-util/get-type-name entity) #"-" "_")]
       (-> (merge {:page_type page-type} doc-data)
           (with-meta doc-meta))
       )))
  ([entity constructor-fn target-id]
   (let [document (constructor-fn entity)
         doc-meta (assoc (.metadata document)
                         :_id target-id)]
     (with-meta (.data document) doc-meta)))
  )

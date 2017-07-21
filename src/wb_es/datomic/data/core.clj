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
            [wb-es.datomic.data.operon :as operon]
            [wb-es.datomic.data.position-matrix :as position-matrix]
            [wb-es.datomic.data.paper :as paper]
            [wb-es.datomic.data.phenotype :as phenotype]
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
  [entity]
  (let [constructor-function
        (case (data-util/get-ident-attr entity)
          :analysis/id analysis/->Analysis
          :anatomy-term/id anatomy-term/->Anatomy-term
          :antibody/id antibody/->Antibody
          :cds/id cds/->Cds
          :clone/id clone/->Clone
          :construct/id construct/->Construct
          :do-term/id do-term/->Do-term
          :expression-cluster/id expression-cluster/->Expression-cluster
          :expr-pattern/id expr-pattern/->Expr-pattern
          :expr-profile/id expr-profile/->Expr-profile
          :feature/id feature/->Feature
          :gene/id gene/->Gene
          :gene-class/id gene-class/->Gene-class
          :go-term/id go-term/->Go-term
          :gene-cluster/id gene-cluster/->Gene-cluster
          :homology-group/id homology-group/->Homology-group
          :interaction/id interaction/->Interaction
          :laboratory/id laboratory/->Laboratory
          :life-stage/id life-stage/->Life-stage
          :molecule/id molecule/->Molecule
          :microarray-results/id microarray-results/->Microarray-results
          :motif/id motif/->Motif
          :operon/id operon/->Operon
          :position-matrix/id position-matrix/->Position-matrix
          :paper/id paper/->Paper
          :phenotype/id phenotype/->Phenotype
          :protein/id protein/->Protein
          :pseudogene/id pseudogene/->Pseudogene
          :rearrangement/id rearrangement/->Rearrangement
          :rnai/id rnai/->Rnai
          :sequence/id sequence/->Sequence
          :strain/id strain/->Strain
          :structure-data/id structure-data/->Structure-data
          :transcript/id transcript/->Transcript
          :transgene/id transgene/->Transgene
          :transposon/id transposon/->Transposon
          :transposon-family/id transposon-family/->Transposon-family
          :variation/id variation/->Variation
          :wbprocess/id wbprocess/->Wbprocess
          (throw (Exception. "Not sure how to handle the data type. Throw an error to let you know")))]
    (constructor-function entity)))

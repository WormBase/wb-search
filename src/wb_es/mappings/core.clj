(ns wb-es.mappings.core)

(defn ref-mapping []
  {:type "nested"
   :properties {:wbid {:type "string"
                       :analyzer "keyword_ignore_case"}
                :label {:type "string"}
                :ref_type {:type "string"}}})

(def default-mapping
  {:properties
   {:wbid {:type "string"
           :analyzer "keyword"
           :include_in_all false}

    :label {:type "string"
            :include_in_all false
            :fields {:raw {:type "string"
                           :analyzer "keyword_ignore_case"}
                     :autocomplete {:type "string"
                                    :analyzer "autocomplete"
                                    :search_analyzer "standard"}}
            }
    :other_names {:type "string"
                  :analyzer "keyword_ignore_case"}
    :paper_type {:type "string"
                 :analyzer "keyword_ignore_case"}
    :species {:type "string"
              :analyzer "keyword_ignore_case"}
    :species_name {:type "string"}

    :genotype {:type "string"}

    ;; start of refs
    :author (ref-mapping)
    :gene (ref-mapping)
    :phenotype (ref-mapping)
    :strain (ref-mapping)
    ;; end of refs
    }})

(def index-settings
  {:settings
   {:number_of_shards 1
    :analysis {:filter {"autocomplete_filter" {:type "edge_ngram"
                                               :min_gram 2
                                               :max_gram 20}}
               :analyzer {"autocomplete" {:type "custom"
                                          :tokenizer "standard"
                                          :filter ["lowercase" "autocomplete_filter"]}
                          "keyword_ignore_case" {:type "custom"
                                                 :tokenizer "keyword"
                                                 :filter ["lowercase"]}}}}
   :mappings {:_default_ default-mapping}})

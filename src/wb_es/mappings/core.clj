(ns wb-es.mappings.core)

(def default-mapping
  {:properties
   {:wbid {:type "string"
           :analyzer "keyword_ignore_case"}

    ;; Used when label isn't available and wbid has human readable parts
    ;; that serve the purpose of a label (for autocompletion and tokenization)
    :wbid_as_label {:type "string"
                    :analyzer "simple"
                    :fields {:autocomplete {:type "string"
                                            :analyzer "autocomplete"
                                            :search_analyzer "standard"}}}
    :label {:type "string"
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
    :gene {:type "string"
           :analyzer "keyword_ignore_case"}}})

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

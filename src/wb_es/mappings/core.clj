(ns wb-es.mappings.core)

(def go-term-mapping
  {:properties
   {:wbid {:type "string"
           :index "not_analyzed"}
    :label {:type "string"
            :analyzer "autocomplete"
            :search_analyzer "standard"}}})

(def index-settings
  {:settings
   {:number_of_shards 1
    :analysis {:filter {"autocomplete_filter" {:type "edge_ngram"
                                               :min_gram 2
                                               :max_gram 20}}
               :analyzer {"autocomplete" {:type "custom"
                                          :tokenizer "standard"
                                          :filter ["autocomplete_filter"]}}}}
   :mappings {:go_term go-term-mapping}})

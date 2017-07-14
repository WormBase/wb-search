(ns wb-es.mappings.core)

(def go-term-mapping
  {:go_term
   {:properties
    {:wbid
     {:type "string"
      :index "not_analyzed"}
     :label
     {:type "string"}}}})

(def index-settings
  {:settings
   {:number_of_shards 1
    :analysis
    {:filter
     {}
     :analyzer
     {}}}})

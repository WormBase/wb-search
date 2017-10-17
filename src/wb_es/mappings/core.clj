(ns wb-es.mappings.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))

(defn ref-mapping []
  {:type "nested"
   :properties {:id {:type "string"
                     :analyzer "keyword"}
                :label {:type "string"}
                :class {:type "string"}}})

(def generic-mapping
  {:properties
   {:wbid {:type "string"
           :analyzer "keyword"
           :include_in_all false
           :fields {:autocomplete_keyword {:type "string"
                                           :analyzer "autocomplete"
                                           :search_analyzer "keyword_ignore_case"}}}

    :label {:type "string"
            :include_in_all false
            :fields {:raw {:type "string"
                           :analyzer "keyword_ignore_case"}
                     :autocomplete {:type "string"
                                    :analyzer "autocomplete"
                                    :search_analyzer "standard"}

                     ;; autocomplete analyzer will handle gene name like unc-22 as phase search,
                     ;; seeems sufficient for now, no need for autocomplete_keyword analyzer
                     :autocomplete_keyword {:type "string"
                                            :analyzer "autocomplete_keyword"
                                            :search_analyzer "keyword_ignore_case"}
                     }
            }
    :other_names {:type "string"
                  :include_in_all false
                  :analyzer "keyword_ignore_case"}
    :page_type {:type "string"
                :analyzer "keyword_ignore_case"}
    :paper_type {:type "string"
                 :analyzer "keyword_ignore_case"}
    :species {:properties
              {:key {:type "string"
                     :analyzer "keyword_ignore_case"}
               :name {:type "string"}}}

    :genotype {:type "string"}

    ;; start of refs
    :allele (ref-mapping)
    :author (ref-mapping)
    :gene (ref-mapping)
    :phenotype (ref-mapping)
    :strain (ref-mapping)
    ;; end of refs
    }})

(def index-settings
  {:settings
   {:analysis {:filter {"autocomplete_filter" {:type "edge_ngram"
                                               :min_gram 2
                                               :max_gram 20}}
               :analyzer {"autocomplete" {:type "custom"
                                          :tokenizer "standard"
                                          :filter ["lowercase" "autocomplete_filter"]}
                          "autocomplete_keyword" {:type "custom"
                                                  :tokenizer "keyword"
                                                  :filter ["lowercase" "autocomplete_filter"]}
                          "keyword_ignore_case" {:type "custom"
                                                 :tokenizer "keyword"
                                                 :filter ["lowercase"]}}}}
   :mappings {:generic generic-mapping}})

(defn create-index
  ([index & {:keys [default-index]}]
   (let [index-url (format "%s/%s " es-base-url index)
         settings (if default-index
                    (assoc-in index-settings :aliases release-id {})
                    index-settings)]
     (http/put index-url {:headers {:content-type "application/json"}
                          :body (json/generate-string settings)}))))

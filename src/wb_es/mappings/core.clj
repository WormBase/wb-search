(ns wb-es.mappings.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))

(defn ref-mapping []
  {:type "nested"
   :properties {:id {:type "keyword"}
                :label {:type "text"
                        :copy_to "other"}
                :class {:type "keyword"}}})

(def generic-mapping
  {:properties
   {:wbid {:type "keyword"
           :normalizer "lowercase_normalizer"
           :fields {:autocomplete_keyword {:type "text"
                                           :analyzer "autocomplete_keyword"
                                           :search_analyzer "keyword_ignore_case"
                                           }}
           }

    :label {:type "text"
            :fields {:raw {:type "keyword"}
                     :autocomplete {:type "text"
                                    :analyzer "autocomplete"
                                    :search_analyzer "standard"}

                     ;; autocomplete analyzer will handle gene name like unc-22 as phase search,
                     ;; seeems sufficient for now, no need for autocomplete_keyword analyzer
                     :autocomplete_keyword {:type "text"
                                            :analyzer "autocomplete_keyword"
                                            :search_analyzer "keyword_ignore_case"}
                     }
            }
    :other_unique_ids {:type "keyword"
                       :normalizer "lowercase_normalizer"}
    :other_names {:type "text"}


    ;; start of copy_to fields
    :description_all {:type "text"}
    :other {:type "text"
            :analyzer "split_underscore_analyzer"}
    ;; end of copy to fields


    :description {:type "text"
                  :copy_to "description_all"}
    :legacy_description {:type "text"
                         :copy_to "description_all"}

    :page_type {:type "keyword"
                :copy_to "other"
                :normalizer "lowercase_normalizer"}
    :paper_type {:type "keyword"
                 :copy_to "other"
                 :normalizer "lowercase_normalizer"}
    :species {:properties
              {:key {:type "keyword"
                     :copy_to "other"
                     :normalizer "lowercase_normalizer"}
               :name {:type "text"
                      :copy_to "other"}}}

    :genotype {:type "text"
               :copy_to "other"}

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
               :normalizer {"lowercase_normalizer" {:type "custom"
                                                    :char_filter []
                                                    :filter ["lowercase"]}}
               :char_filter
               {"replace_underscore"
                {:type "mapping"
                 :mappings ["_ => -"]}}

               :analyzer {"autocomplete" {:type "custom"
                                          :tokenizer "standard"
                                          :filter ["lowercase" "autocomplete_filter"]}
                          "autocomplete_keyword" {:type "custom"
                                                  :tokenizer "keyword"
                                                  :filter ["lowercase" "autocomplete_filter"]}
                          "keyword_ignore_case" {:type "custom"
                                                 :tokenizer "keyword"
                                                 :filter ["lowercase"]}
                          "split_underscore_analyzer"
                          {:char_filter ["replace_underscore"]
                           :tokenizer "standard"}}}}
   :mappings {:_doc generic-mapping}})

(defn create-index
  ([index & {:keys [default-index delete-existing]}]
     (let [index-url (format "%s/%s " es-base-url index)
           settings (if default-index
                      (assoc-in index-settings [:aliases release-id] {})
                      index-settings)]
       (do
         (if delete-existing
           (try
             (http/delete index-url)
             (catch clojure.lang.ExceptionInfo e
               (if-not (= (:status (ex-data e))
                          404)
                 (clojure.pprint/pprint (ex-data e))))))
         (try
           (http/put index-url {:headers {:content-type "application/json"}
                                :body (json/generate-string settings)})
           (catch clojure.lang.ExceptionInfo e
             (clojure.pprint/pprint (ex-data e))
             (throw e))))
       )))

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

(def default-mapping
  {:properties
   {:wbid {:type "keyword"
           :normalizer "lowercase_normalizer"
           :fields {:autocomplete_keyword {:type "text"
                                           :analyzer "autocomplete_keyword"
                                           :search_analyzer "keyword_ignore_case"
                                           }}
           }

    :label {:type "text"
            :fields {:raw {:type "keyword"
                           :normalizer "lowercase_normalizer"}
                     :english {:type "text"
                               :analyzer "english"}
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
    :other_names {:type "text"
                  :fields
                  {:raw
                   {:type "keyword"
                    :normalizer "lowercase_normalizer"}}}


    ;; start of copy_to fields
    :categories_all {:type "text"
                     :analyzer "split_underscore_analyzer"}
    :description_all {:type "text"
                      :fields
                      {:english
                       {:type "text"
                        :analyzer "english"}}
                      :store true}
    :other {:type "text"}
    ;; end of copy to fields


    :description {:type "text"
                  :copy_to "description_all"}
    :legacy_description {:type "text"
                         :copy_to "description_all"}

    :page_type {:type "keyword"
                :copy_to "categories_all"
                :normalizer "lowercase_normalizer"}
    :paper_type {:type "keyword"
                 :copy_to "categories_all"
                 :normalizer "lowercase_normalizer"}
    :species {:properties
              {:key {:type "keyword"
                     :copy_to "categories_all"
                     :normalizer "lowercase_normalizer"}
               :name {:type "text"
                      :copy_to "categories_all"}}}

    :genotype {:type "text"
               :copy_to "other"}
    :biological_process {:type "keyword"
                         :normalizer "lowercase_normalizer"}
    :cellular_component {:type "keyword"
                         :normalizer "lowercase_normalizer"}
    :molecular_function {:type "keyword"
                         :normalizer "lowercase_normalizer"}

    ;; start of refs
    :allele (ref-mapping)
    :author (ref-mapping)
    :gene (ref-mapping)
    :phenotype (ref-mapping)
    :strain (ref-mapping)
    ;; end of refs

    ;; for interaction only

    :interaction_type_physical {:type "keyword"
                                :normalizer "lowercase_normalizer"}
    :interaction_type_genetic {:type "keyword"
                               :normalizer "lowercase_normalizer"}
    :interaction_type_regulartory {:type "keyword"
                                   :normalizer "lowercase_normalizer"}
    :interaction_type_predicted {:type "keyword"
                                 :normalizer "lowercase_normalizer"}
    :interaction_type_gi-module-one {:type "keyword"
                                     :normalizer "lowercase_normalizer"}
    :interaction_type_gi-module-two {:type "keyword"
                                     :normalizer "lowercase_normalizer"}
    :interaction_type_gi-module-three {:type "keyword"
                                       :normalizer "lowercase_normalizer"}


    :join {:type "join"
           :relations {"interaction_group" "interaction"}}
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
   :mappings {:_doc default-mapping}})

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

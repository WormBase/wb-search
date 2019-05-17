(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn wrap-query-lower-case [handler]
  (fn [request]
    (handler (update-in request [:params :q] #(some-> % clojure.string/lower-case)))))

;; TODO use whitelist instead of blacklist
(def ^:private non-filter-parameters
  #{:size
    :from
    :explain
    :page
    :query
    :q
    :autocomplete
    :raw
    })

(defn get-filter [options]
  (->> options
       (remove (fn [[key value]]
                 (or (non-filter-parameters key)
                     (not value))))
       (map (fn [[key value]]
              (let [normalized-value (some->> value
                                              (clojure.string/lower-case))
                    term-or-terms-key (if (sequential? value)
                                        :terms
                                        :term)]
                (case key

                  :type
                  {term-or-terms-key {:page_type value}}

                  :species
                  {term-or-terms-key {:species.key value}}

                  {term-or-terms-key {key value}}
                  ))))
       ))

(declare autocomplete)

(defn- compose-search-query [q options]
  (if (and q (not= (clojure.string/trim q) ""))
    {:bool
     {:filter (get-filter options)
      :should [{:dis_max
                {:boost 2
                 :queries [{:term {:wbid {:value q
                                          :boost 5} }}
                           {:term {:label.raw {:value q
                                               :boost 5}}}
                           {:term {:other_names.raw {:value q
                                                     :boost 4}}}
                           {:match_phrase {:label {:query q :slop 12}}}
                           {:match_phrase {:label.english {:query q :slop 12}}}
                           {:match_phrase {:other_names {:query q
                                                         :boost 0.8}}}
                           {:match_phrase {:description_all {:query q
                                                             :boost 0.2}}}
                           {:match_phrase {:description_all.english {:query q
                                                                     :boost 0.2}}}
                           ]
                 }}
               {:match_phrase {:categories_all {:query q}}}
               {:match_phrase {:other {:query q
                                       :boost 0.1}}}]
      :minimum_should_match 1}}
    {:bool {:must (get-filter options)}}))

(defn search [es-base-url index q options]
  (if (:autocomplete options)
    (autocomplete es-base-url index q options)
    (let [query {;:explain true
                 :sort [:_score
                        {:label.raw {:order :asc}}]
                 :query
                 {:function_score
                  {:query (compose-search-query q options)
                   :boost_mode "multiply"
                   :score_mode "multiply"
                   :functions
                   [{:weight 2
                     :filter
                     {:term {:species.key {:value "c_elegans"}}}}
                    {:weight 2
                     :filter
                     {:bool
                      {:must_not
                       {:exists
                        {:field :species.key}}}}}
                    {:weight 0.1
                     :filter
                     {:bool
                      {:must_not
                       [{:exists
                         {:field :label}}]}}}]}}
                 :highlight
                 {:fields {:wbid {}
                           :wbid_as_label {}
                           :label {}
                           :other_names {}
                           :description {}}}
                 }

          response
          (try
            (http/get (format "%s/%s/_search?size=%s&from=%s&explain=%s"
                              es-base-url
                              index
                              (get options :size 10)
                              (get options :from 0)
                              (get options :explain false))
                      {:content-type "application/json"
                       :body (json/generate-string query)})
            (catch clojure.lang.ExceptionInfo e
              (clojure.pprint/pprint query)
              (clojure.pprint/pprint (ex-data e))
              (throw e)))]
      (json/parse-string (:body response) true))))


(defn autocomplete [es-base-url index q options]
  (let [query {:sort [:_score
                      {:label.raw {:order :asc}}]
               :query
               {:function_score
                {:query
                 {:bool
                  {:filter (get-filter options)
                   :should [{:term {:wbid.autocomplete_keyword q}}
                            {:term {:label.autocomplete_keyword q}}
                            {:match_phrase {:label.autocomplete {:query q
                                                                 :slop 12}}}]
                   :minimum_should_match 1}}
                 :boost_mode "replace"
                 :score_mode "multiply"
                 :functions
                 [{:weight 1
                   :filter
                   {:match_all {}}}
                  {:weight 2
                   :filter
                   {:term {:species.key {:value "c_elegans"}}}}
                  {:weight 2
                   :filter
                   {:bool
                    {:must_not
                     {:exists
                      {:field :species.key}}}}}
                  {:weight 0.8
                   :filter
                   {:bool
                    {:must
                     [{:term {:wbid.autocomplete_keyword q}}]}}}
                  {:weight 0.1
                   :filter
                   {:bool
                    {:must_not
                     [{:term {:label.autocomplete_keyword q}}]
                     :must
                     [{:match_phrase {:label.autocomplete {:query q
                                                           :slop 12}}}]}}}]}}}

        response
        (http/get (format "%s/%s/_search?size=%s&explain=%s"
                          es-base-url
                          index
                          (get options :size 10)
                          (get options :explain false))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn search-exact [es-base-url index q options]
  (let [query {:query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:term {:wbid q}}
                                   {:term {:other_unique_ids q}}
                                   {:term {"label.raw" q}}]}}]}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn random [es-base-url index options]
  (let [date-seed (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") (new java.util.Date))
        query {:query
               {:function_score
                {:filter {:bool {:filter (get-filter options)}}
                 :functions [{:random_score {:seed date-seed}}]
                 :score_mode "sum"}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn count [es-base-url index q options]
  (if (:autocomplete options)
    (autocomplete es-base-url index q options)
    (let [query
          {:query (compose-search-query q options)}

          response
          (http/get (format "%s/%s/_count"
                            es-base-url
                            index)
                    {:content-type "application/json"
                     :body (json/generate-string query)})]
      (json/parse-string (:body response) true))))

(defn- interaction-type-query [type-string]
  (let [segments (clojure.string/split type-string #":")
        musts (filter #(re-matches #"[^-].+" %) segments)
        must-nots (keep (fn [segment]
                          (let [[_ type] (re-matches #"-(.+)" segment)]
                            type))
                        segments)
        get-rule (fn [interaction-type]
                   {:has_child
                    {:type "interaction"
                     "query" {:exists {:field (format "interaction_type_%s" interaction-type)}}}})]
    {:bool {:must (map get-rule musts)
            :must_not (map get-rule must-nots)}}
))

(defn facets [es-base-url index q options]
  (let [categories-config (case (:type options)
                            "paper"
                            [{:field :paper_type}]

                            "interaction_group"
                            [{:field :biological_process}
                             {:field :cellular_component}
                             {:field :interaction_type_genetic
                              :option :genetic_interaction_subtypes
                              :child_type "interaction"}
                             {:field :interaction_type_physical
                              :option :physical_interaction_subtypes
                              :child_type "interaction"}
                             {:option :interaction_type
                              :aggs {:categories
                                     {:filters
                                      {:filters
                                       (->>
                                        ["physical:genetic", "physical:-genetic", "-physical:genetic"]
                                        (map (fn [type-string]
                                               [type-string (interaction-type-query type-string)]))
                                        (into {}))}}}}
                             ]

                            [{:field :page_type
                              :option :type}
                             {:field :species.key
                              :option :species}])

        get-category-option (fn [category-config]
                              (or (:option category-config)
                                  (:field category-config)))
        ;; options that do not interfere with the facet categories
        non-category-options (->> categories-config
                                  (keep get-category-option)
                                  (apply dissoc options))
        request-body {:query (compose-search-query q non-category-options)
                      :size 0
                      :aggs (reduce (fn [result category]
                                      (let [option (get-category-option category)
                                            field (:field category)
                                            child-type (:child_type category)]
                                        (assoc result option {:filter {:bool {:filter (get-filter (dissoc options option))}}
                                                              :aggs (cond
                                                                     (:aggs category)
                                                                     (:aggs category)

                                                                     child-type
                                                                     {:children
                                                                      {:children {:type child-type}
                                                                       :aggs {:categories
                                                                              {:terms {:field field}
                                                                               :aggs {:parent
                                                                                      {:parent {:type child-type}}}}}}}

                                                                     :else
                                                                     {:categories
                                                                      {:terms {:field field}}})})))
                                    {}
                                    categories-config)}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string request-body)})]
    (json/parse-string (:body response) true)))

(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn wrap-query-lower-case [handler]
  (fn [request]
    (handler (update-in request [:params :q] #(some-> % clojure.string/lower-case)))))


(defn get-filter [options]
  (->> []
       (cons (when-let [type-value (:type options)]
               {:term {:page_type type-value}}))
       (cons (when-let [species-value (some->> (:species options)
                                               (clojure.string/lower-case))]
               {:term {:species.key species-value}}))
       (cons (when-let [species-value (some->> (:paper_type options)
                                               (clojure.string/lower-case))]
               {:term {:paper_type species-value}}))
       (filter identity)))


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
                           {:match_phrase {:label {:query q}}}
                           {:match_phrase {:label.english {:query q}}}
                           {:match_phrase {:other_names {:query q
                                                         :boost 0.8}}}
                           {:match_phrase {:description_all {:query q
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
                        {:field :species.key}}}}}]}}
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
                            {:term {:label.autocomplete q}}]
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
                  {:weight 0.01
                   :filter
                   {:term {:label.autocomplete q}}}]}}}

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

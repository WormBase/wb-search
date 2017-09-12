(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn wrap-query-lower-case [handler]
  (fn [request]
    (let [q-raw (get-in request [:params :q])
          new-request (-> request
                          (update-in [:params :q] #(some-> % clojure.string/lower-case))
                          (assoc-in [:params :q_raw] q-raw)
                          )]
      (handler new-request))
    ))


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


(defn search [es-base-url index q options]
  (let [query (if (and q (not= (clojure.string/trim q) ""))
                {;:explain true
                 :query
                 {:bool
                  {:must [{:bool {:filter (get-filter options)}}
                          {:dis_max
                           {:queries [{:term {:wbid (:q_raw options)}}
                                      {:match_phrase {:label {:query q
                                                              :minimum_should_match "70%"}}}
                                      {:match_phrase {:other_names {:query q
                                                                    :minimum_should_match "70%"
                                                                    :boost 0.9}}}
                                      {:match_phrase {:_all {:query q
                                                             :minimum_should_match "70%"
                                                             :boost 0.1}}}]
                            :tie_breaker 0.3}}]}}
                 :highlight
                 {:fields {:wbid {}
                           :wbid_as_label {}
                           :label {}
                           :other_names {}
                           :description {}}}}
                {:query {:bool {:filter (get-filter options)}}})

        response
        (http/get (format "%s/%s/_search?size=%s&from=%s"
                          es-base-url
                          index
                          (get options :size 10)
                          (get options :from 0))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn autocomplete [es-base-url index q options]
  (let [query {:sort [:_score
                      {:label {:order :asc}}]
               :query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:match {:wbid.autocomplete_keyword q}}
                                   {:bool {:should [{:match {:label.autocomplete_keyword q}}
                                                    {:match {:label.autocomplete q}}]}}]}}]}}}

        response
        (http/get (format "%s/%s/_search?size=%s"
                          es-base-url
                          index
                          (get options :size 10))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn search-exact [es-base-url index q options]
  (let [query {:query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:term {:wbid q}}
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
  (let [query (if (and q (not= (clojure.string/trim q) ""))
                {:query
                 {:bool
                  {:must [{:bool {:filter (get-filter options)}}
                          {:dis_max
                           {:queries [{:term {:wbid (:q_raw options)}}
                                      {:match_phrase {:label {:query q
                                                              :minimum_should_match "70%"}}}
                                      {:match_phrase {:other_names {:query q
                                                                    :minimum_should_match "70%"
                                                                    :boost 0.9}}}
                                      {:match_phrase {:_all {:query q
                                                             :minimum_should_match "70%"
                                                             :boost 0.1}}}]
                            :tie_breaker 0.3}}]}}}
                {:query {:bool {:filter (get-filter options)}}})

        response
        (http/get (format "%s/%s/_count"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))

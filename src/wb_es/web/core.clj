(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))

(defn get-filter [options]
  (->> []
       (cons (when-let [type-value (:type options)]
               {:type {:value (clojure.string/replace type-value #"_" "-")}}))
       (cons (when-let [species-value (:species options)]
               {:term {:species species-value}}))
       (filter identity)))


(defn search [q options]
  (let [query (if (and q (not= (clojure.string/trim q) ""))
                {:query
                 {:bool
                  {:must [{:bool {:filter (get-filter options)}}
                          {:dis_max
                           {:queries [{:term {:wbid q}}
                                      {:match {:label q}}
                                      {:match {:_all {:query q :boost 0.1}}}]
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
                          release-id
                          (get options :size 10)
                          (get options :from 0))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn autocomplete [q options]
  (let [query {:query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:match {:label.autocomplete q}}
                                   {:match {:wbid_as_label q}}]}}]}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          release-id)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn search-exact [q options]
  (let [query {:query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:term {:wbid q}}
                                   {:term {"label.raw" q}}]}}]}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          release-id)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn random [options]
  (let [date-seed (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") (new java.util.Date))
        query {:query
               {:function_score
                {:filter {:bool {:filter (get-filter options)}}
                 :functions [{:random_score {:seed date-seed}}]
                 :score_mode "sum"}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          release-id)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn count [q options]
  (let [query (if (and q (not= (clojure.string/trim q) ""))
                {:query
                 {:bool
                  {:must [{:bool {:filter (get-filter options)}}
                          {:dis_max
                           {:queries [{:term {:wbid q}}
                                      {:match {:label q}}
                                      {:match {:_all {:query q :boost 0.1}}}]
                            :tie_breaker 0.3}}]}}}
                {:query {:bool {:filter (get-filter options)}}})

        response
        (http/get (format "%s/%s/_count"
                          es-base-url
                          release-id)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))

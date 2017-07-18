(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))

(defn get-filter [options]
  )

(defn search [q options]
  (let [query {:query
               {:bool
                {:should [{:term {:wbid q}}
                          {:match {:label q}}
                          {:match {:_all q}}]}}}

        response
        (http/get (format "%s/%s/_search?size=%s&from=%s"
                          es-base-url
                          release-id
                          (get options :size 10)
                          (get options :from 0))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response))))


(defn autocomplete [q options]
  (let [query {:query
               {:bool
                {:should [{:match {:label q}}]}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          release-id)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response))))


(defn search-exact [q options]
  (let [query {:query
               {:bool
                {:should [{:term {:wbid q}}
                          {:term {"label.raw" q}}]}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          release-id)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response))))

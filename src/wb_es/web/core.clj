(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]))

(defn search [q options]
  (let [query {:query {:match {:label q}}}

        response
        (http/get (format "%s/%s/_search?size=%s&from=%s"
                          es-base-url
                          release-id
                          (get options :size 10)
                          (get options :from 0))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response))))

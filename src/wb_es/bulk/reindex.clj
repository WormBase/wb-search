(ns wb-es.bulk.reindex
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.tools.cli :refer [parse-opts]]
            [cheshire.core :as json]
            [wb-es.env :refer [es-base-url release-id]]
            [wb-es.mappings.core :refer [create-index]]))

(defn reindex
  [old-index new-index]
  (http/post (format "%s/_reindex" es-base-url)
             {:headers {:content-type "application/json"}
              :body (json/generate-string {:source {:index old-index}
                                          :dest {:index new-index}})}))

(defn update-alias
  [old-index new-index]
  (http/post (format "%s/_aliases" es-base-url)
             {:headers {:content-type "application/json"}
              :body (json/generate-string {:actions [{:remove {:index old-index
                                                               :alias release-id}}
                                                     {:add {:index new-index
                                                            :alias release-id}}]})}))


(def cli-options
  [["-s" "--steal-alias" "Steal the release id alias from the source index"]])

(defn -main
  "reindex from one index into another"
  [& args]
  (let [parsed-options (parse-opts args cli-options)
        [old-index new-index] (:arguments parsed-options)]
    (if (and old-index new-index)
      (do
        (create-index new-index)
        ;;      (reindex old-index new-index)
        (if (:steal-alias parsed-options)
          (update-alias old-index new-index)))
      (throw (Exception. "Please provide source index and destination index as CLI arguments")))))

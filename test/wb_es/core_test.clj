(ns wb-es.core-test
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.test :refer :all]
   [datomic.api :as d]
   [mount.core :as mount]
   [wb-es.bulk.core :as bulk]
   [wb-es.datomic.data.core :refer [create-document]]
   [wb-es.datomic.db :refer [datomic-conn]]
   [wb-es.env :refer [es-base-url]]
   [wb-es.mappings.core :as mappings]
   [wb-es.web.core :as web])
  )

(def index-name "test")

(defn wrap-setup [f]
  (do
    (let [index-url (format "%s/%s" es-base-url index-name)]
      (do
        (http/delete index-url)
        (http/put index-url {:headers {:content-type "application/json"}
                             :body (json/generate-string mappings/index-settings)})
        (mount/start)
        (f)
        (mount/stop))
      )))

(use-fixtures :once wrap-setup)

(defn index-docs [& docs]
  (let [formatted-docs (bulk/format-bulk "index" "test" docs)]
    (bulk/submit formatted-docs :refresh true)))

(defn index-datomic-entity [entity]
  (->> entity
       (create-document)
       (conj [])
       (apply index-docs)))

(defn with-index [search-func]
  (fn [& search-args]
    (let [parameterized-search-func (partial search-func es-base-url index-name)]
      (apply parameterized-search-func search-args))))

(def search (with-index web/search))
(def autocomplete (with-index web/autocomplete))


(deftest autocomplete-test
  (let [db (d/db datomic-conn)]
    (testing "autocomplete by clone name"
      (do
        (index-datomic-entity (d/entity db [:clone/id "W02C12"]))
        (let [first-hit (->> (search "W02C12" {})
                             :hits
                             :hits
                             (first))]
          (is (= "W02C12" (get-in first-hit [:_source :wbid])))
          (is (= "clone" (get-in first-hit [:_type])))))
      )))

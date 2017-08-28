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

(defn index-doc [& docs]
  (let [formatted-docs (bulk/format-bulk "index" "test" docs)]
    (bulk/submit formatted-docs :refresh true)))

(defn index-datomic-entity [& entities]
  (->> entities
       (map create-document)
       (apply index-doc)))

(defn with-default-options
  "create a new search func that "
  [search-func]
  (fn [& search-args]
    (let [[q options] search-args]
      (search-func es-base-url
                   index-name
                   q
                   (or options {})))))

(def search (with-default-options web/search))
(def autocomplete (with-default-options web/autocomplete))


(deftest anatomy-type-test
  (testing "anatomy type using \"tl\" prefixed terms"
    (let [db (d/db datomic-conn)
          anatomy-tl-prefixed (->> (d/q '[:find [?e ...]
                                          :in $ [?th ...]
                                          :where
                                          [?e :anatomy-term/term ?th]]
                                        db
                                        (->> (d/datoms db :aevt :anatomy-term.term/text)
                                             (keep (fn [[e _ v]]
                                                     (if (re-matches #"tl.*" (clojure.string/lower-case v))
                                                       e)))))
                                   (map (partial d/entity db))
                                   (shuffle))]
      (do
        (apply index-datomic-entity anatomy-tl-prefixed)
        (testing "autocomplete by term"
          (let [hits (-> (autocomplete "tl")
                         (get-in [:hits :hits]))]
            (is (= "TL"
                   (->> (get-in (first hits) [:_source :label]))))

            (is (some (fn [hit]
                        (= "TL.a"
                           (get-in hit [:_source :label])))
                      hits))
            )))
      )))


(deftest clone-type-test
  (let [db (d/db datomic-conn)]
    (testing "clone type using W02C12 as example"
      (do
        (index-datomic-entity (d/entity db [:clone/id "W02C12"]))
        (testing "search by clone WBID"
          (let [first-hit (->> (search "W02C12")
                               :hits
                               :hits
                               (first))]
            (is (= "W02C12" (get-in first-hit [:_source :wbid])))
            (is (= "clone" (get-in first-hit [:_type])))))
        (testing "autocomplete by clone WBID"
          (let [hits (->> (autocomplete "W02C")
                          :hits
                          :hits)]
            (is (some (fn [hit]
                        (= "W02C12" (get-in hit [:_source :wbid])))
                      hits)))
          )))
    ))

(deftest disease-type-test
  (testing "disease type using \"park\" as an example"
    (let [db (d/db datomic-conn)
          disease-parks (->> (d/datoms db :aevt :do-term/name)
                             (keep (fn [[e _ v]]
                                     (if (re-matches #".*park.*" (clojure.string/lower-case v))
                                       e)))
                             (map (partial d/entity db))
                             (shuffle))]
      (testing "autocomplete by disease name"
        (do
          (apply index-datomic-entity disease-parks)
          (let [hits (-> (autocomplete "park" {:size (count disease-parks)})
                         (get-in [:hits :hits]))]

            (testing "match Parkinson's disease"
              (some (fn [hit]
                      (= "Parkinson's disease"
                         (get-in hit [:_source :label])))
                    hits))
            (testing "matching early-onset Parkinson disease"
              (some (fn [hit]
                      (= "early-onset Parkinson disease"
                         (get-in hit [:_source :label])))
                    hits))
            (testing "X-linked dystonia-parkinsonism"
              (some (fn [hit]
                      (= "X-linked dystonia-parkinsonism"
                         (get-in hit [:_source :label])))
                    hits))

            ))))
    ))

(deftest transgene-type-test
  (testing "transgene using syis1 as example"
    (let [db (d/db datomic-conn)
          transgenes-prefixed-syis1 (->> (d/datoms db :aevt :transgene/public-name)
                                         (keep (fn [[e _ v]]
                                                 (if (re-matches #"syIs1.*" v)
                                                   e)))
                                         (map (partial d/entity db))
                                         (shuffle))]

      (testing "autocompletion by transgene name"
        (do
          (apply index-datomic-entity transgenes-prefixed-syis1))
          (let [hits (-> (autocomplete "syIs1")
                         (get-in [:hits :hits]))]
            (testing "result appears in autocompletion"
              (is (some (fn [hit]
                          (= "syIs101"
                             (get-in hit [:_source :label])))
                        hits)))
            (testing "result in right order"
              (is (= "syIs1" (-> (first hits)
                                 (get-in [:_source :label])))))))

      (testing "autocompletion by transgene name in lowercase"
        (do
          (apply index-datomic-entity transgenes-prefixed-syis1)
          (let [hits (-> (autocomplete "syis1")
                         (get-in [:hits :hits]))]
            (is (some (fn [hit]
                        (= "syIs101"
                           (get-in hit [:_source :label])))
                      hits)))))
      )))

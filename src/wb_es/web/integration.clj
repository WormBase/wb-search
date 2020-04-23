(ns wb-es.web.integration)

(defn- pack-search-obj [doc]
  (let [doc-source (:_source doc)]
    {:id (:wbid doc-source)
     :class (clojure.string/replace (get-in doc [:_source :page_type]) "-" "_")
     :label (or (:label doc-source)
                (:wbid doc-source))
     :other_names (:other_names doc-source)
     :taxonomy (get-in doc-source [:species :key])
     :highlight (:highlight doc)}))

(defn- pack-species [species-name]
  (if species-name
    (let [[genus-name species-name] (clojure.string/split species-name #"\s+" 2)]
      {:genus genus-name
       :species species-name
       :id (format "%s_%s"
                   (->> genus-name
                        (first)
                        (clojure.string/lower-case))
                   species-name)})))

(defn- get-obj [doc]
  (let [search-obj (pack-search-obj doc)
        doc-source (:_source doc)

        non-empty-doc-source
        (reduce (fn [result [k v]]
                  (let [non-empty-v
                        (if (sequential? v)
                          (not-empty v)
                          v)]
                    (assoc result k non-empty-v)))
                {}
                doc-source)]
    (assoc non-empty-doc-source
           :name search-obj
           :taxonomy (->> (get-in doc-source [:species :name])
                          (pack-species)))))

(defn wrap-params [handler]
  (fn [request]
    (let [params (:params request)
          raw_page_count (get params :page 1)
          page (if (= raw_page_count "all") 1 raw_page_count)
          page-size (if (= raw_page_count "all") 500 (get params :page_size 10))
          raw_q (some-> (or (:id params)
                            (:query params)
                            (:q params))
                        (clojure.string/trim))
          params-new (assoc params
                            :size page-size
                            :from (->> page
                                       (Integer.)
                                       (dec)
                                       (* page-size))
                            :q (if raw_q (clojure.string/replace raw_q #"\*" ""))
                            :autocomplete (if (and raw_q
                                                   (re-matches #".+\*" raw_q))
                                            true)
                            :type (get params :type (:class params))
                            :species (:species params)
                            :raw params)]
      ;;(prn (select-keys request [:uri :params]))
      (handler (assoc request :params params-new)))))

(defn wrap-search [handler]
  (fn [request]
    (let [response (handler request)
          body-new {:matches (->> (get-in response [:body :hits :hits])
                                  (map get-obj))
                    :query (get-in request [:params :q])
                    :querytime (-> (get-in response [:body :took])
                                   (/ 1000.0))
                    :count (get-in response [:body :hits :total])
                    :page (get-in request [:params :raw :page])
                    :page_size (get-in request [:params :raw :page_size])}]
      (assoc response :body body-new))))

(defn wrap-autocomplete [handler]
  (fn [request]
    (let [response (handler request)
          body-new {:struct (->> (get-in response [:body :hits :hits])
                                 (map (fn [doc]
                                        (let [packed-species (->> (get-in doc [:_source :species :name])
                                                                  (pack-species))]
                                          (-> (pack-search-obj doc)
                                              (assoc :taxonomy packed-species))))))
                    :query (get-in request [:params :q])
                    :page 1
                    :page_size (get-in request [:params :raw :page_size])}]
      (assoc response :body body-new))))

(defn wrap-search-exact [handler]
  (fn [request]
    (let [params (:params request)
          response (handler request)
          pack-function get-obj
          body (get-in response [:body :hits :hits])
          exact-match (if (= 1 (count body))
                        (first body))]
      (assoc response :body (if exact-match
                              (if (or (:fill params)
                                      (:footer params))
                                (assoc (get-obj exact-match) :footer (get-in request [:params :footer]))
                                (pack-search-obj exact-match))
                              {})))))

(defn wrap-count [handler]
  (fn [request]
    (let [response (handler request)
          count (get-in response [:body :count])]
      (assoc response :body {:count count}))))

(defn wrap-random [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc response :body (->> (get-in response [:body :hits :hits])
                                 (first)
                                 (get-obj))))))

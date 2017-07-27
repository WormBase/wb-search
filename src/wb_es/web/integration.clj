(ns wb-es.web.integration)

(defn- pack-search-obj [doc]
  (let [doc-source (:_source doc)]
    {:id (:wbid doc-source)
     :class (clojure.string/replace (:_type doc) "-" "_")
     :label (:label doc-source)
     :taxonomy (:species doc-source)}))

(defn- get-obj [doc]
  (let [search-obj (pack-search-obj doc)
        doc-source (:_source doc)]
    {:name search-obj
     :description (:description doc-source)
     :taxonomy nil}))

(defn wrap-search [handler]
  (fn [request]
    (let [params (:params request)
          page (get params :page 1)
          page_size (get params :page_size 10)
          params-new (assoc params
                            :size page_size
                            :from (->> page
                                       (Integer.)
                                       (dec)
                                       (* page_size)))
          request-new (assoc request :params params-new)
          response (handler request-new)
          body-new {
                    :matches (->> (get-in response [:body :hits :hits])
                                  (map get-obj))
                    :query (:q params)
                    :querytime (->> (get-in response [:body :took])
                                    (Integer.)
                                    (#(/ % 1000.0)))
                    :count (get-in response [:body :hits :total])
                    :page page
                    :page_size page_size}]
      (assoc response :body body-new))))

(defn wrap-autocomplete [handler]
  handler)

(defn wrap-search-exact [handler]
  (fn [request]
    (let [response (handler request)
          content-new (->> (get-in response [:body :hits :hits])
                           (map pack-search-obj)
                           (first))]
      (assoc response :body content-new))))

(defn wrap-count [handler]
  handler)

(defn wrap-random [handler]
  handler)

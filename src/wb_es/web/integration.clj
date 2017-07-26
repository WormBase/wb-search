(ns wb-es.web.integration)

(defn- pack-search-obj [doc]
  (let [doc-source (:_source doc)]
    {:id (:wbid doc-source)
     :class (clojure.string/replace (:_type doc) "-" "_")
     :label (:label doc-source)
     :taxonomy (:species doc-source)}))

(defn wrap-search [handler]
  handler)

(defn wrap-autocomplete [handler]
  handler)

(defn wrap-search-exact [handler]
  (fn [request]
    (let [response (handler request)
          content-new (->> (get-in response [:body :hits :hits])
                           (map pack-search-obj)
                           (first))]
      (do (prn request) (assoc response :body content-new)))))

(defn wrap-count [handler]
  handler)

(defn wrap-random [handler]
  handler)

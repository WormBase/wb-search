(ns wb-es.web.integration)

(defn wrap-search [handler]
  handler)

(defn wrap-autocomplete [handler]
  handler)

(defn wrap-search-exact [handler]
  (fn [request]
    (let [response (handler request)
          new-response (assoc-in response [:body "aaaa"] "aaaaa")]
      (do (prn response)
          (prn new-response)
          new-response))))

(defn wrap-count [handler]
  handler)

(defn wrap-random [handler]
  handler)

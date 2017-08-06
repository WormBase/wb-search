(ns wb-es.web.index
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response not-found]]
            [wb-es.web.core :as web-core]
            [wb-es.web.integration :as web-integration]))

(def search-route
  (GET "/search" [q & options]
       (response (web-core/search q options))))

(def autocomplete-route
  (GET "/autocomplete" [q & options]
       (response (web-core/autocomplete q options))))

(def search-exact-route
  (GET "/search-exact" [q & options]
       (response (web-core/search-exact q options))))

(def count-route
  (GET "/count" [q & options]
       (response (web-core/count q options))))

(def random-route
  (GET "/random" [q & options]
       (response (web-core/random options))))

(def api-lite-routes
  (->
    (context "/" []
      search-route
      autocomplete-route
      search-exact-route
      count-route
      random-route)
    (wrap-routes web-core/wrap-query-lower-case)))

(defroutes integration-routes
  (-> search-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-search))
  (-> autocomplete-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-autocomplete))
  (-> search-exact-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-search-exact))
  (-> count-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-count))
  (-> random-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-random)))

(defroutes app
  api-lite-routes
  (context "/integration" []
           integration-routes
           (context "/lite" []
                    (-> api-lite-routes
                        (wrap-routes web-integration/wrap-params)))
  (route/not-found (response {:message "endpoint not found"}))))


(defn handler [request]
  (let [enhanced-handler
        (-> app
            (wrap-json-response {:pretty true})
            (wrap-defaults api-defaults))]
    (enhanced-handler request)))

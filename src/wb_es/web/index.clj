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

(defroutes app
  search-route
  autocomplete-route
  search-exact-route
  count-route
  random-route
  (context "/integration" []
           (web-integration/wrap-search search-route)
           (web-integration/wrap-autocomplete autocomplete-route)
           (web-integration/wrap-search-exact search-exact-route)
           (web-integration/wrap-count count-route)
           (web-integration/wrap-random random-route))
  (route/not-found (response {:message "endpoint not found"})))

(defn handler [request]
  (let [enhanced-handler
        (-> app
            (wrap-json-response {:pretty true})
            (wrap-defaults api-defaults))]
    (enhanced-handler request)))

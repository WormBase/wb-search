(ns wb-es.web.index
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response not-found]]
            [wb-es.web.core :as web-core]))

(defroutes app
  (GET "/search" [q & options]
       (response (web-core/search q options)))
  (GET "/autocomplete" [q & options]
       (response (web-core/autocomplete q options)))
  (GET "/search-exact" [q & options]
       (response (web-core/search-exact q options)))
  (route/not-found (response {:message "endpoint not found"})))

(defn handler [request]
  (let [enhanced-handler
        (-> app
            (wrap-json-response {:pretty true})
            (wrap-defaults api-defaults))]
    (enhanced-handler request)))

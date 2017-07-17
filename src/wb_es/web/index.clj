(ns wb-es.web.index
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response not-found]]))

(defroutes app
  (GET "/search" [q species type paper_type]
       (response {:species species}))
  (route/not-found (response {:message "endpoint not found"})))

(defn handler [request]
  (let [enhanced-handler
        (-> app
            (wrap-json-response)
            (wrap-defaults api-defaults))]
    (enhanced-handler request)))

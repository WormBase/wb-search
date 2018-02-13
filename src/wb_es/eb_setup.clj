(ns wb-es.eb-setup
  (:require [cheshire.core :as json])
  (:gen-class))

(defn- replace-image-tag [image tag json-str]
  (let [pattern (re-pattern (format "(\\Q%s\\E):[^\"]+" image))]
    (clojure.string/replace json-str pattern
                            (fn [[_ group1]]
                              (format "%s:%s" group1 tag)))))

(defn update-eb-json! []
  (let [version (System/getProperty "wb-es.version")
        eb-json-path "Dockerrun.aws.json"
        eb-json-str (slurp eb-json-path)]
    (->> eb-json-str
         (replace-image-tag "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/aws-elasticsearch" version)
         (replace-image-tag "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-web-api" version)
         (spit eb-json-path))
    ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (update-eb-json!))

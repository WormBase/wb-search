(ns wb-es.eb-setup
  (:require [cheshire.core :as json])
  (:gen-class))

(defn- replace-image-tag [image tag json-str]
  (let [pattern (re-pattern (format "(\\Q%s\\E):[^\"]+" image))]
    (clojure.string/replace json-str pattern
                            (fn [[_ group1]]
                              (format "%s:%s" group1 tag)))))

(defn update-eb-json! [eb-json-path]
  (let [version (System/getProperty "wb-es.version")
        eb-json-str (slurp eb-json-path)]
    (->> eb-json-str
         (replace-image-tag "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/aws-elasticsearch" version)
         (replace-image-tag "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-web-api" version)
         (spit eb-json-path))
    ))

(defn find-eb-json [path]
  (->> (clojure.java.io/file path)
       (file-seq)
       (filter #(= "Dockerrun.aws.json" (.getName %)))
       (map #(.getAbsoluteFile %))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [eb-json-paths (find-eb-json "./eb")]
    (doseq [eb-json-path eb-json-paths]
      (update-eb-json! eb-json-path))))

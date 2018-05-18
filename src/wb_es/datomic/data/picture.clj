(ns wb-es.datomic.data.picture
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Picture [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:picture/id entity)
     :label (some->> (:picture/reference entity)
                     (first)
                     (:paper/brief-citation)
                     (format "Picture from %s"))
     :description (first (:picture/description entity))}))

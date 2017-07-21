;; Public_name
;; other-name
;; Summary
(ns wb-es.datomic.data.wbprocess
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Wbprocess [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:wbprocess/id entity)
     :label (:wbprocess/public-name entity)
     :other_names (:wbprocess/other-name entity)
     :description (->> entity
                       (:wbprocess/summary)
                       (:wbprocess.summary/text))}))

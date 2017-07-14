(ns wb-es.datomic.data.util
  (:require [wb-es.env :refer [release-id]]))

(defprotocol Document
  (metadata [this])
  (data [this]))

(defn get-ident-attr
  "get the ident attribute of a datomic entity"
  [entity]
  (->> (keys entity)
       (filter #(= (name %) "id"))
       (first)))

(defn get-type-name
  "get the schema name of a datomic entity"
  [entity]
  (if-let [ident-attr (get-ident-attr entity)]
    (namespace ident-attr)))

(defn default-metadata
  "default implementation of the data method of Document protocol"
  [entity]
  (let [ident (get-ident-attr entity)
        type (get-type-name entity)]
    (if ident
      {:_index release-id
       :_type type
       :_id (format "%s:%s" type (ident entity))}
      (throw (Exception. "cannot identify ident attribute of the entity")))))

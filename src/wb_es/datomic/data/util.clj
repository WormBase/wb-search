(ns wb-es.datomic.data.util
  (:require [wb-es.env :refer [release-id]]
            [datomic.api :as d]))

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
  {:_type "generic"
   :_id (:db/id entity)})

(defn format-enum
  "format the datomic enum into a elasticsearch term"
  [enum]
  (-> (name enum)
      (clojure.string/replace #"-" "_")))

(defn format-species-enum
  "format a species entity as a elasticsearch term"
  ([species-entity] (format-species-enum species-entity nil))
  ([species-entity bioproject-id]
   (if species-entity
     (let [[genus-name species-name] (-> (:species/id species-entity)
                                         (clojure.string/lower-case)
                                         (clojure.string/split #"\s+"))
           genus-short (str (first genus-name))]
       (if bioproject-id
         (format "%s_%s_%s" genus-short species-name bioproject-id)
         (format "%s_%s" genus-short species-name))))))

(def format-species-enum-memoized (memoize format-species-enum))

(defn- bioprojec-to-strain [db bioproject-id]
  (d/q '[:find ?sid .
         :in $ ?b
         :where
         [?c :sequence-collection.database/accession ?b]
         [?e :sequence-collection/database ?c]
         [?e :sequence-collection/strain ?s]
         [?s :strain/id ?sid]]
       db bioproject-id))

(defn format-species-text
  "format the species as full text"
  ([species-entity] (format-species-text species-entity nil))
  ([species-entity bioproject-id]
   (if species-entity
     (if bioproject-id
       ;; (format "%s (%s)" (:species/id species-entity) (bioprojec-to-strain (d/entity-db species-entity) bioproject-id)) ;; broken in ws259
       (format "%s (%s)" (:species/id species-entity) bioproject-id) ;; temp fix for ws259
       (:species/id species-entity)))))

(def format-species-text-memoized (memoize format-species-text))

(defmulti format-entity-species
  "format the species as an object"
  (fn [species-key entity] species-key))

(defmethod format-entity-species :default [species-key entity]
  (if-let [species-entity (species-key entity)]
    (let [entity-id-attr (keyword (namespace species-key) "id")
          [_ bioproject-id] (re-matches #"(PRJ.*):.*" (entity-id-attr entity))]
      {:key (format-species-enum-memoized species-entity bioproject-id)
       :name (format-species-text-memoized species-entity bioproject-id)})))


;;
;; START of a section lifted out of WormBase/datomic-to-catalyst
;; https://github.com/WormBase/datomic-to-catalyst/blob/develop/src/rest_api/formatters/object.clj
;;
;;

(declare pack-obj)

(defmulti obj-label
  "Build a human-readable label for `obj`"
  (fn [class obj] class))

(defmethod obj-label "gene" [_ obj]
  (or (:gene/public-name obj)
      (:gene/id obj)))

(defmethod obj-label "wbprocess" [_ obj]
  (or (:wbprocess/public-name obj)
      (:wbprocess/id obj)))

(defmethod obj-label "laboratory" [_ obj]
  (or (first (:laboratory/mail obj))
      (:laboratory/id obj)))

(defmethod obj-label "phenotype" [_ obj]
  (or (->> (:phenotype/primary-name obj)
           (:phenotype.primary-name/text))
      (:phenotype/id obj)))

(defmethod obj-label "molecule" [_ obj]
   (or (first (:molecule/public-name obj))
      (:molecule/id obj)))

(defmethod obj-label "variation" [_ obj]
  (or (:variation/public-name obj)
      (:variation/id obj)))

;; Helpers for paper labels.
;; (defn- author-lastname [author-holder]
;;   (or
;;    (->> (:affiliation/person author-holder)
;;         (first)
;;         (:person/last-name))
;;    (-> (:paper.author/author author-holder)
;;        (:author/id)
;;        (.trim)
;;        (str/split #"\s+")
;;        (first))))


;; (defmethod obj-label "paper" [_ paper]
;;   (if-let [year (when (seq (:paper/publication-date paper))
;;                   (first
;;                     (str/split
;;                       (:paper/publication-date paper)
;;                       #"-")))]
;;     (str (author-list paper) ", " year)
;;     (author-list paper)))

(defmethod obj-label "feature" [_ feature]
  (or (:feature/public-name feature)
      (if (nil? (:feature/other-name feature))
        (:feature/id feature)
        (first (:feature/other-name feature)))))

(defmethod obj-label "anatomy-term" [_ term]
  (or (:anatomy-term.term/text (:anatomy-term/term term))
      (:anatomy-term/id term)))

(defmethod obj-label "do-term" [_ term]
  (:do-term/name term))

(defmethod obj-label "person" [_ person]
 (or (:person/standard-name person)
     (or (:author/id (first (:person/possibly-publishes-as person)))
         (:person/id person))))

(defmethod obj-label "construct" [_ cons]
  (or (first (:construct/public-name cons))
      (or (first (:construct/other-name cons))
          (:construct/id cons))))

(defmethod obj-label "transgene" [_ tg]
  (or (:transgene/public-name tg)
      (:transgene/id tg)))

(defmethod obj-label "go-term" [_ go]
  (first (:go-term/name go))) ;; Not clear why multiples allowed here!

(defmethod obj-label "life-stage" [_ ls]
  (:life-stage/public-name ls))

(defmethod obj-label "molecule-affected" [_ ls]
  (:moluecule/public-name ls))

(defmethod obj-label "protein" [_ prot]
  (or (first (:protein/gene-name prot))
      (:protein/id prot)))

;; (def q-interactor
;;   '[:find [?interactor ...]
;;     :in $ ?int
;;     :where
;;     (or-join
;;      [?int ?interactor]
;;      (and
;;       [?int
;;        :interaction/interactor-overlapping-gene ?gi]
;;       [?gi
;;        :interaction.interactor-overlapping-gene/gene
;;        ?interactor])
;;      (and
;;       [?int :interaction/molecule-interactor ?mi]
;;       [?mi
;;        :interaction.molecule-interactor/molecule
;;        ?interactor])
;;      (and
;;       [?int :interaction/other-interactor ?orint]
;;       [?orint
;;        :interaction.other-interactor/text
;;        ?interactor])
;;      (and
;;       [?int :interaction/rearrangement ?ri]
;;       [?ri
;;        :interaction.rearrangement/rearrangement
;;        ?interactor])
;;      (and
;;       [?int :interaction/feature-interactor ?fi]
;;       [?fi
;;        :interaction.feature-interactor/feature
;;        ?interactor]))])

;; (defmethod obj-label "interaction" [_ int]
;;   ;; Note that only certain types of interactor are considered when
;;   ;; computing the display name.
;;   (let [db (d/entity-db int)]
;;     (if-let [il (seq (d/q q-interactor db (:db/id int)))]
;;       (->> il
;;            (map
;;             (fn [interactor]
;;               (cond
;;                 (string? interactor)
;;                 interactor

;;                 :default
;;                 (:label (pack-obj (d/entity db interactor))))))
;;            (sort)
;;            (str/join " : "))
;;       (:interaction/id int))))

(defmethod obj-label "motif" [_ motif]
  (or (first (:motif/title motif))
      (:motif/id motif)))

(defmethod obj-label :default [class obj]
  ((keyword class "id") obj))

(defn obj-class
  "Attempt to determine the class of a WormBase-ish entity-map."
  [obj]
  (cond
   (:gene/id obj)
   "gene"

   (:clone/id obj)
   "clone"

   (:cds/id obj)
   "cds"

   (:protein/id obj)
   "protein"

   (:feature/id obj)
   "feature"

   (:rearrangement/id obj)
   "rearrangement"

   (:variation/id obj)
   "variation"

   (:anatomy-term/id obj)
   "anatomy-term"

   (:molecule/id obj)
   "molecule"

   (:life-stage/id obj)
   "life-stage"

   (:go-term/id obj)
   "go-term"

   :default
   (if-let [k (first (filter #(= (name %) "id") (keys obj)))]
     (namespace k))))

(defn pack-obj
  "Retrieve a 'packed' (web-API) representation of entity-map `obj`."
  ([obj]
   (pack-obj (obj-class obj) obj))
  ([class obj & {:keys [label]}]
   (if obj
     {:id ((keyword class "id") obj)
      :label (or label (obj-label class obj))
      :class (if class
               (if (= class "author")
                 "person"
                 (clojure.string/replace class "-" "_")))})))


;;
;; END of a section lifted out of WormBase/datomic-to-cataly
;;

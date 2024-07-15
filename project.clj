(defproject wb-es "2.9.9-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.7.0"
  :sign-releases false
  :dependencies
  [[cheshire "5.10.0"]
   [clj-http "3.12.3"]
   [org.clojure/core.async "1.3.618"]
   [org.clojure/tools.cli "1.0.206"]
   [environ "1.2.0"]
   [mount "0.1.16"]
   [org.clojure/clojure "1.10.3"]
   [factual/durable-queue "0.1.5"]
   [com.taoensso/timbre "5.1.2"]

   ;; Update ElasticSearch client to 7.x
   [org.elasticsearch.client/elasticsearch-rest-high-level-client "7.17.9"]

   ;; the following dependencies are only needed for web
   [compojure "1.6.2"]
   [ring/ring-defaults "0.3.3"]
   [ring/ring-core "1.9.5"]
   [ring/ring-json "0.5.1"]
   ;; use jetty in tests
   [ring/ring-jetty-adapter "1.9.5"]]

  :source-paths ["src"]
  :plugins [[lein-environ "1.2.0"]
            [lein-pprint "1.3.2"]]
  :main ^:skip-aot wb-es.core
;  :resource-paths ["resources" "file:datomic-repository/datomic-pro"]
  :target-path "target/%s"
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "GPLv2"
  :jvm-opts ["-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"
             "-Ddatomic.txTimeoutMsec=1000000"]
  :profiles
  {:datomic-free
   {:dependencies [[com.datomic/datomic-free "0.9.5561.56"
                    :exclusions [joda-time]]]}
   :datomic-pro
   {:dependencies [[com.datomic/datomic-pro "1.0.7180"
                    :exclusions [joda-time]]]}
   :ddb
   {:dependencies
    [[com.amazonaws/aws-java-sdk-dynamodb "1.12.261"
      :exclusions [joda-time]]]}
   :search-engine
   {:docker {:image-name "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/aws-elasticsearch"
             :dockerfile "docker/Dockerfile.aws-elasticsearch"}}
   :indexer [:datomic-pro :ddb
             {:main wb-es.bulk.core
              :uberjar-name "wb-es-indexer-standalone.jar"
              :docker {:image-name "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-indexer"
                       :dockerfile "docker/Dockerfile.indexer"}
              }]
   :web [:datomic-free
         {:uberjar-name "wb-es-web-standalone.jar"
          :ring {:handler wb-es.web.index/handler
                 :init wb-es.web.setup/run}
          :docker {:image-name "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-web-api"
                   :dockerfile "docker/Dockerfile.web"}}]
   :web-dev {:dependencies [[ring/ring-devel "1.9.5"]]
             :plugins [[lein-ring "0.12.6"]]}
   :dev [:indexer
         :web
         :web-dev
         {:aliases
          {"code-qa"
           ["do"
            ["eastwood"]
            "test"]}
          :source-paths ["dev"]
          :env
          {:wb-db-uri "datomic:ddb://us-east-1/WS260/wormbase"
           :swagger-validator-url "http://localhost:8002"}
          :plugins
          [[jonase/eastwood "1.2.4"
            :exclusions [org.clojure/clojure]]
           [lein-ancient "0.7.0"]
           [lein-bikeshed "0.5.2"]
           [lein-ns-dep-graph "0.2.0-SNAPSHOT"]
           [venantius/yagni "0.1.7"]
           [com.jakemccrary/lein-test-refresh "0.25.0"]
           [io.sarnowski/lein-docker "1.1.0"]
           [lein-shell "0.5.0"]]}]
   :uberjar {:aot :all}
   :test
   {:resource-paths ["test/resources"]}}
  :aliases {"test" ["with-profile" "+indexer,+web" "test"]
            "test-refresh" ["with-profile" "+indexer,+web" "test-refresh"]
            "aws-ecr-publish" ["do"
                               ["shell" "make" "aws-ecr-login"]
                               ["with-profile" "search-engine" "docker" "build"]
                               ["with-profile" "search-engine" "docker" "push"]
                               ["with-profile" "indexer" "uberjar"]
                               ["with-profile" "indexer" "docker" "build"]
                               ["with-profile" "indexer" "docker" "push"]
                               ["with-profile" "web" "ring" "uberjar"]
                               ["with-profile" "web" "docker" "build"]
                               ["with-profile" "web" "docker" "push"]]
            "eb-container-version-update" ["run" "-m" "wb-es.eb-setup"]}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["eb-container-version-update"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["aws-ecr-publish"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :repl-options {:init (do
                         (set! *print-length* 10)
                         (use 'wb-es.env)
                         (use 'wb-es.datomic.db)
                         (require '[datomic.api :as d])
                         (mount.core/start))})
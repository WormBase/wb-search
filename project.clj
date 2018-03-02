(defproject wb-es "0.2.6"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.7.0"
  :sign-releases false
  :dependencies
  [[cheshire "5.7.0"]
   [clj-http "2.3.0"]
   [org.clojure/core.async "0.3.442"]
   [org.clojure/tools.cli "0.3.5"]
   [environ "1.1.0"]
   [mount "0.1.11"]
   [org.clojure/clojure "1.8.0"]

   ;; the following dependecies are only needed for web
   ]
  :source-paths ["src"]
  :plugins [[lein-environ "1.1.0"]
            [lein-pprint "1.1.1"]]
  :main ^:skip-aot wb-es.core
  :resource-paths ["resources"]
  :target-path "target/%s"
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "GPLv2"
  :jvm-opts ["-Xmx2G"
             ;; same GC options as the transactor,
             ;; should minimize long pauses.
             "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"
             "-Ddatomic.objectCacheMax=1000000000"
             "-Ddatomic.txTimeoutMsec=1000000"]
  :profiles
  {:datomic-free
   {:dependencies [[com.datomic/datomic-free "0.9.5561.56"
                    :exclusions [joda-time]]]}
   :datomic-pro
   {:dependencies [[com.datomic/datomic-pro "0.9.5561.56"
                    :exclusions [joda-time]]]}
   :ddb
   {:dependencies
    [[com.amazonaws/aws-java-sdk-dynamodb "1.11.82"
      :exclusions [joda-time]]]}
   :indexer [:datomic-pro :ddb
             {:docker {:image-name "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/aws-elasticsearch"
                       :dockerfile "docker/Dockerfile.aws-elasticsearch"}}]
   :web [:datomic-free
         {:uberjar-name "wb-es-web-standalone.jar"
          :dependencies [[compojure "1.6.0"]
                         [ring/ring-defaults "0.3.0"]
                         [ring/ring-core "1.6.2"]
                         [ring/ring-json "0.4.0"]
                         ;; use jetty in tests
                         [ring/ring-jetty-adapter "1.6.3"]]
          :ring {:handler wb-es.web.index/handler
                 :init wb-es.web.setup/run}
          :docker {:image-name "357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-web-api"
                   :dockerfile "docker/Dockerfile.web"}}]
   :web-dev {:dependencies [[ring/ring-devel "1.5.1"]]
             :plugins [[lein-ring "0.12.0"]]}
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
          [[jonase/eastwood "0.2.4"
            :exclusions [org.clojure/clojure]]
           [lein-ancient "0.6.8"]
           [lein-bikeshed "0.3.0"]
           [lein-ns-dep-graph "0.1.0-SNAPSHOT"]
           [venantius/yagni "0.1.4"]
           [com.jakemccrary/lein-test-refresh "0.17.0"]
           [io.sarnowski/lein-docker "1.1.0"]
           [lein-shell "0.5.0"]]}]
   :uberjar {:aot :all}
   :test
   {:resource-paths ["test/resources"]}}
  :aliases {"test" ["with-profile" "+indexer,+web" "test"]
            "test-refresh" ["with-profile" "+indexer,+web" "test-refresh"]}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["run" "-m" "wb-es.eb-setup"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v"]
                  ["shell" "make" "aws-ecr-login"]
                  ["with-profile" "indexer" "docker" "build"]
                  ["with-profile" "indexer" "docker" "push"]
                  ["with-profile" "web" "ring" "uberjar"]
                  ["with-profile" "web" "docker" "build"]
                  ["with-profile" "web" "docker" "push"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :repl-options {:init (do
                         (set! *print-length* 10)
                         (use 'wb-es.env)
                         (use 'wb-es.datomic.db)
                         (require '[datomic.api :as d])
                         (mount.core/start))})

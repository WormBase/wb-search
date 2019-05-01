
all: docker-build-web

.PHONY: uberjar-build-web
uberjar-build-web:
	lein with-profile web ring uberjar

.PHONY: docker-build-web
docker-build-web: uberjar-build-web
	docker build -t wormbase/search-web-api -f ./docker/Dockerfile.web .

.PHONY: docker-run-web
docker-run-web:
	docker run -p 9006:3000 -e WB_DB_URI=${WB_DB_URI} wormbase/search-web-api

.PHONY: uberjar-build-indexer
uberjar-build-indexer:
	lein with-profile indexer uberjar

.PHONY: docker-build-indexer
docker-build-indexer: uberjar-build-indexer
	docker build -t wormbase/search-indexer -f ./docker/Dockerfile.indexer .

.PHONY: docker-run-indexer
docker-run-indexer:
	docker run \
		-e WB_DB_URI=${WB_DB_URI} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY} \
		wormbase/search-indexer

.PHONY: docker-build-aws-es
docker-build-aws-es:
	docker build -t wormbase/aws-elasticsearch -f ./docker/Dockerfile.aws-elasticsearch .

.PHONY: docker-run-aws-es
docker-run-aws-es:
	@docker rm -f elasticsearch
	@docker run -p 9200:9200 \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY} \
		--ulimit nofile=65536:65536 \
		-v /var/lib/elasticsearch/data:/usr/share/elasticsearch/data \
		--network wb-network \
		--name elasticsearch \
		wormbase/aws-elasticsearch

.PHONY: docker-run-kibana
docker-run-kibana:
	@docker run -p 5601:5601 \
		--network=wb-network \
		docker.elastic.co/kibana/kibana

.PHONY: eb-local-run
eb-local-run:
	$(MAKE) -C ./eb/default eb-local-run

.PHONY: aws-ecr-login
aws-ecr-login:
	aws ecr get-login --no-include-email --region us-east-1 | sh

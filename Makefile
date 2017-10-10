all: docker-build-web

.PHONY: uberjar-build-web
uberjar-build-web:
	lein with-profile web ring uberjar

.PHONY: docker-build-web
docker-build-web: uberjar-build-web
	docker build -t wb-es-web -f ./docker/Dockerfile.web .

.PHONY: docker-run-web
docker-run-web:
	docker run -p 3000:3000 -p 2900:2900 wb-es-web

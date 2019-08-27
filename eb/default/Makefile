WS_VERSION ?= $(shell echo ${WB_DB_URI} | sed -rn 's|.*(WS[0-9]+).*|\1|p')
LOWER_WS_VERSION ?= $(shell echo ${WS_VERSION} | tr A-Z a-z)
RESTORE_FROM_SNAPSHOT ?= latest
ENV_NAME_SUFFIX ?= $(shell git rev-parse --short HEAD)

.PHONY: eb-local-run
eb-local-run:
	@eb local run --envvars AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID},AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY},WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=${RESTORE_FROM_SNAPSHOT}

.PHONY: eb-setenv
eb-setenv:
	@eb setenv WB_DB_URI=${WB_DB_URI} RESTORE_FROM_SNAPSHOT=${RESTORE_FROM_SNAPSHOT}

.PHONY: eb-create
eb-create:
	@eb create wormbase-search-${LOWER_WS_VERSION}-${ENV_NAME_SUFFIX} --cfg v1.0.0 --cname wormbase-search-${LOWER_WS_VERSION} --envvars WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=${RESTORE_FROM_SNAPSHOT}

.PHONY: eb-create-hotfix
eb-create-hotfix:
	@eb create wormbase-search-${LOWER_WS_VERSION}-${ENV_NAME_SUFFIX} --cfg v1.0.0 --cname wormbase-search-hotfix --envvars WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=${RESTORE_FROM_SNAPSHOT}

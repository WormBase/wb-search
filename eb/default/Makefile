WS_VERSION ?= $(shell echo ${WB_DB_URI} | sed -rn 's|.*(WS[0-9]+).*|\1|p')
LOWER_WS_VERSION ?= $(shell echo ${WS_VERSION} | tr A-Z a-z)

.PHONY: eb-local-run
eb-local-run:
	@eb local run --envvars AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID},AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY},WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=latest

.PHONY: eb-setenv
eb-setenv:
	@eb setenv AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY} WB_DB_URI=${WB_DB_URI} RESTORE_FROM_SNAPSHOT=latest

.PHONY: eb-create
eb-create:
	@eb create wormbase-search-${LOWER_WS_VERSION} --cfg v0.2.14 --cname wormbase-search-${LOWER_WS_VERSION} --envvars AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID},AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY},WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=latest

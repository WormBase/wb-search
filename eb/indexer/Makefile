.PHONY: eb-setenv
eb-setenv:
	@eb setenv AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY} WB_DB_URI=${WB_DB_URI} RESTORE_FROM_SNAPSHOT=

.PHONY: eb-create
eb-create:
	@eb create wormbase-search-indexer --cfg v0.2.9 --cname wormbase-search-indexer --envvars AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID},AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY},WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=

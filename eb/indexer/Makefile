.PHONY: eb-setenv
eb-setenv:
	@eb setenv WB_DB_URI=${WB_DB_URI} RESTORE_FROM_SNAPSHOT=

.PHONY: eb-create
eb-create:
	@eb create wormbase-search-indexer --cfg v2 --cname wormbase-search-indexer --envvars WB_DB_URI=${WB_DB_URI},RESTORE_FROM_SNAPSHOT=

# WormBase search service

**This repository is intended for WormBase developers**,
who would like to build and deploy services that power the search functionality
on the WormBase webiste.

## Architecture overview
The search service stack consists of:
- A search database (or index) along with the data
- A Web API

WormBase search database is currently re-build for each release.
The final product is a the search service stack deployed on AWS Elastic Beanstalk,
as a multi-container setup.

This process involves multiple steps along with multiple tools and configurations,
which are hosted in this repository.


## Setting up

**For initial setup, it might be easiest to do so at the rest-dev server,
where everything other than Datomic-pro has been installed**

You will need to **install**:
- Java 8
- Leiningen 2.x
- Datomic-pro
- Docker
- AWS CLI
- Elastic Beanstalk CLI

You will need to obtain various **AWS permissions** for:
- S3 bucket wormbase-elasticsearch-snapshots
- WormBase Datomic/DynamoDB database
- Elastic Beanstalk / Starting and stopping EC2

Sensitive info are communicated to the programs through **environment variables**.
You will need to set the following environment variables:

- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- WB_DB_URI

## Production environment

Ensure the environment variables above are set appropriately before preceeding.

### Deploy Indexer
```
(cd eb/indexer/ && make eb-create)
```
- Indexer is programmed to save a snapshot of the index to the snapshot repository on S3 when it finishes indexing. [Read more](#step-2-create-index-snapshot)
- Remember to manually shut down beanstalk instance after the indexer finishes running and a snapshot is saved.


### Deploy Web API
```
(cd eb/default/ && make eb-create)
```

To make a production-like environment locally, `(cd eb/default/ && make eb-local-run)`.

## Development environment

Start Elasticsearch:

- refer to [Step 0: Start Elasticsearch](#step-0-start-elasticsearch)

Run indexer if necessary:

- refer to [Step 1: Build index](#step-1-build-index)

Starting web API for development:
```
lein trampoline ring server-headless [port]
```
- If no index of the appropriate version is found locally, it will attempt to restore the index from the snapshot repository on s3.

Starting automated tests:
```
lein trampoline test-refresh
```

## Release containers

_Note: a release of containers *may not* be necessary for every WS release. In general, we only need to release containers when source code changes._

```
make aws-ecr-login
lein release [$LEVEL]
```

- [Learn more about release levels](https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#releasing-simplified)
- [Learn more about the custom release steps](https://github.com/WormBase/wb-search/blob/develop/project.clj)


## (Optional) Build and depoloy search *manually*
_Not recommended except for development and troubleshooting_

### Step 0: Start Elasticsearch
#### Start Elasticsearch from our custom Docker image for Elasticsearch
```
make docker-build-aws-es
make docker-run-aws-es
```
An Elasticsearch will be started at http://localhost:9200

### Step 1: Build index

Building indexing creates a new search database (index) from either
1) the primary database (such as Datomic) or
2) an existing index

#### To create an index from primary database:
```
lein trampoline run -m wb-es.bulk.core
```
It creates an index named wsXXX_v0 with an alias wsXXX.

#### Or, to create an index from existing database (or to re-index)
such as when the Elasticsearch mapping is changed
```
lein trampoline run -m wb-es.bulk.reindex [SOURCE_INDEX_NAME] [TARGET_INDEX_NAME] --steal-alias
```
It creates a new index by the name [TARGET_INDEX_NAME]. Please follow the convention wsXXX_v[number] for naming an index.
--steal-alias or -s reassigns the alias of the source index to the new index.

### Step 2: Create index snapshot

A snapshot of an index needs to be stored on S3 and will later be restored into Elasticsearch.

#### First, with Elasticsearch running, connect to the S3:
```
lein trampoline run -m wb-es.web.setup
```

The script would also attempts to restore into Elasticsearch the latest snapshot matching the release (as per WB_DB_URI).
And it might fail when such a snapshot doesn't exist. This needs to be fixed in the future.

#### Second, create and post a snapshot to S3:

```
curl -XPUT 'localhost:9200/_snapshot/s3_repository/snapshot_wsXXX_vX?pretty' -H 'Content-Type: application/json' -d'
{
  "indices": "wsXXX"
}
'
```

- Folling the snapshot id convention snapshot_wsXXX_vX is necessary here.
- The index to include is referred by either is alias wsXXX or id wsXXX_vX
- A new snapshot takes sometime before becoming available. Its progress can be tracked with:
`curl -XGET localhost:9200/_cat/snapshots/s3_repository`

For more details about snapshot and restore: https://www.elastic.co/guide/en/elasticsearch/reference/2.4/modules-snapshots.html

### Step 3: Deploy Search service stack

#### Build and commit deployable containers

Containers run by Beanstalk are first build, tagged, and commited to AWS Container Registery.
Two containers needs to be prepared: Elasticsearch and the Web API.

```
make aws-ecr-login
```

For Elasticsearch:
```
make docker-build-aws-es
docker tag wormbase/aws-elasticsearch:latest 357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/aws-elasticsearch:[tag]
docker push 357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/aws-elasticsearch:[tag]
```

Note:
- `[tag]`of a container should match the git tag used to tag a commit,
this will help locating the container image based on git history.
- `[tag]` may be `latest` during development, but any container image intended for production must have a proper tag,
with a matching git tag.

For the Web API
```
make docker-build-web
docker tag wormbase/search-web-api:latest 357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-web-api:[tag]
docker push 357210185381.dkr.ecr.us-east-1.amazonaws.com/wormbase/search-web-api:[tag]
```

#### Update Dockerrun.aws.json

Dockerrun.aws.json describes how various containers are put together to form a system.
It's the Beanstalk equivelent of Docker Compose's compose file.

The versions for docker images needs to be updated to matching the containers created above.

You may also update settings related memory, network, volume, environment variables etc.

#### Prepare environment for Elastic Beanstalk

A Beanstalk enviroment is an EC2 instance managed by Beanstalk.
The easiest way to create a Beanstalk environemt for search is to clone and modify an existing one.

Cloning can be done through the AWS web console or `eb clone`.

Check the new environment is created: `eb list`

Switch to the newly created environment `eb use [environment_name]`.
Subsequenct `eb` commands such as `eb deploy` and `eb setenv` will operate on the new environment.

Set the environment variables for the Beanstalk environment:
`make eb-setenv`

#### Deploy and monitor

Deploy containers to the Beanstalk environemnt: `eb deploy`. (Note: the web API will attempt to restore the latest index of a particular release. This will take a few minutes before the web API finishes restarting.)

You can moniter the health and metrics of the Beanstalk environment on AWS web console under Beanstalk and CloudWatch.

## Miscellaneous

## License

Copyright © 2017 WormBase

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

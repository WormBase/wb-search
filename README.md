WormBase Search Service
====================================================

**This repository is intended for WormBase developers**,
who would like to build and deploy services that power the search functionality
on the WormBase webiste.

Architecture overview
---------------------------------------------------

The search service stack consists of:
- A search database (or index) along with the data
- A Web API

WormBase search database is currently re-build for each release.
The final product is a the search service stack deployed on AWS Elastic Beanstalk,
as a multi-container setup.

This process involves multiple steps along with multiple tools and configurations,
which are hosted in this repository.


Setting up
---------------------------------------------------

### Software Dependencies

**To get started with developing or deploying the search service, 
it is recommanded to do so on the shared development server,
where everything other than Datomic-pro has been installed.**

For more info on installing Datomic Pro, please refer to [the Datomic site](https://docs.datomic.com/on-prem/getting-started/get-datomic.html). The version of Datomic Pro installed should match the version of Datomic Pro specified in [project.clj(project.clj).

**If installing everything from scratch**, here is the software dependencies:
- Java 8
- Leiningen 2.x
- Datomic-pro
- Docker
- AWS CLI
- Elastic Beanstalk CLI


### Environment Variables

You will need to set the following environment variables, and keep them up to date with a WS release. Consider placing them in your `.bash_profile`.

*The Datomic transactor. It looks something like `datomic:ddb://us-east-1/WSXXX/wormbase`*

- **WB_DB_URI** (required)

Optional environment variables for local/Non-AWS development environments only)
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY

### Permissions

For development, appropriate AWS permission is setup on the development server instance through an [instance profile](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html). The permissions are granted to the role `wb-web-team-dev-instance-role` that is attached to the development server instance.

For production, the instance profile role `wb-search-beanstalk-ec2-role` is used by instances created by Beanstalk.

The permissions being granted include:
- S3 bucket wormbase-elasticsearch-snapshots
- WormBase Datomic/DynamoDB database
- Elastic Beanstalk / Starting and stopping EC2


Production environment
---------------------------------------------------

Production environment runs docker containers in Elastic Beanstalk environment on AWS.

We maintain two production environment,

- one for the on-demand indexing process, that runs once per WB release, and
- the other for the long-running Web API, that runs of a Elasticsearch snapshot.

### Preparation for deployment

**Build and upload containers**

Build and upload containers (if and only if source code has changed) and verify Dockerrun.aws.json is updated to use the right tag for the containers.

(_Note: Building containers isn't necessary for every WS release, if the source code hasn't changed._)

```
LEVEL=[major|minor|patch|rc|beta|alpha] make release
```

- [Learn more about release levels](https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#releasing-simplified)
- [Learn more about the custom release steps](https://github.com/WormBase/wb-search/blob/develop/project.clj)


**Set environment variables**

Ensure the environment variables above are set appropriately before preceeding.  You might consider adding them to your `.bash_profile`.

### Deploy Indexer

**Adjust DynamoDB read capacity**

Since running the indexer requires reading from the Datomic database, which uses DynamoDB as its storage layer, the DynamoDB read capacity needs to be increased. 400 units for read should be a good value to try.  You can do this in the AWS console under `Services > DynamoDB > Tables`. Select the appropriate database, and under the `Capacity` tab, set the Provisioned Capacity read units to `400`.

**Run the Indexer**

```
cd wb-search
(cd eb/indexer/ && make eb-create)
```
- The Indexer is programmed to save a snapshot of the index to the snapshot repository on S3 when it finishes indexing. [Read more](#step-2-create-index-snapshot)

*It will take several minutes for the indexer environment to launch.*

- Remember to manually shut down the beanstalk instance after the indexer finishes running and a snapshot is saved.


### Deploy Web API

Deploy the web API to confirm the indexer has worked.

```
(cd eb/default/ && make eb-create)
```

To make a production-like environment locally, `(cd eb/default/ && make eb-local-run)`.

**Troubleshooot tip:**
- If `ERROR: InvalidProfileError - The config profile (eb-cli) could not be found` occurs while running on the development server instance, running `eb init` in the appropriate directory, ie. `eb/indexer/` or `eb/default`, seems to fix that. Remember that instance profile rather than user profile is used on the development server instance.

### Deploy Hotfix

Critical bug fixes are sometimes applied outside of the schedule releases to the production environment.

Applying a hotfix can be achieved by creating a production-like hotfix environment of the web API and swapping its URL with the current production environment.

```
(cd eb/default/ && make eb-create-hotfix)
```

Wait until the new environment responds to the old URL AND **the old DNS record has expired** before terminating the old environment.

If re-indexing is required, deploy the indexer as [shown above](#deploy-indexer), **before** creating the hotfix environment.

## Development environment

Filesystem

Elasticsearch index takes up extra disk space. A volume is mounted to shared development server for this purpose.

Here are the steps to make the volume available for elasticsearch:

```
# after mounting the volume to the server
lsblk # to identify the available volume
sudo mount /dev/xvdg /mnt/elasticsearch_data/   #replace xvdg with the correct volumn
sudo ln -s /mnt/elasticsearch_data/elasticsearch/ elasticsearch
```

Start Elasticsearch:

- refer to [Step 0: Start Elasticsearch](#step-0-start-elasticsearch)

Test-driven development:
- Testing the web api and indexer is made easier and more efficient with [test cases](test/wb_es/core_test.clj).
- Run tests that re-run when code changes

```
lein trampoline test-refresh
```


(Optional) Run indexer:

- refer to [Step 1: Build index](#step-1-build-index)

(Optional) Starting web API for development:
```
lein trampoline ring server-headless [port]
```
- If no index of the appropriate version is found locally, it will attempt to restore the index from the snapshot repository on s3.


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

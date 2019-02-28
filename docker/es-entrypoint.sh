#!/bin/sh
set -e

bin/elasticsearch-keystore create

# credentials for AWS S3 repository plugin
echo ${AWS_ACCESS_KEY_ID} | bin/elasticsearch-keystore add --stdin s3.client.default.access_key
echo ${AWS_SECRET_KEY} | bin/elasticsearch-keystore add --stdin s3.client.default.secret_key

# credentials for AWS EC2 discovery plugin
echo ${AWS_ACCESS_KEY_ID} | bin/elasticsearch-keystore add --stdin discovery.ec2.access_key
echo ${AWS_SECRET_KEY} | bin/elasticsearch-keystore add --stdin discovery.ec2.secret_key

# sudo su
# ulimit -n 65536
# su elasticsearchb

/usr/local/bin/docker-entrypoint.sh

exec "$@"

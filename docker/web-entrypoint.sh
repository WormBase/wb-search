#!/bin/sh
set -e

hostip=$(ip route show | awk '/default/ {print $3}')
ES_BASE_URI="${ES_BASE_URI:-hostip:9200}"

exec "$@"

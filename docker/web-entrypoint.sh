#!/bin/sh
set -e

hostip=$(ip route show | awk '/default/ {print $3}')
export ES_BASE_URI="${ES_BASE_URI:-http://$hostip:9200}"

exec "$@"

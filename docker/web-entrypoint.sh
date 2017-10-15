#!/bin/sh
set -e

hostip=$(ip route show | awk '/default/ {print $3}')
echo $hostip
${ES_BASE_URI:=$hostip}

exec "$@"

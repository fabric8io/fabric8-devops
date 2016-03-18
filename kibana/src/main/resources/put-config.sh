#!/bin/bash

set -exo pipefail

ELASTICSEARCH_URL=${ELASTICSEARCH_URL:-http://localhost:9200}

if [ -n "${ELASTICSEARCH_SERVICE_NAME}" ]; then
    SVC_HOST=${ELASTICSEARCH_SERVICE_NAME}_SERVICE_HOST
    SVC_PORT=${ELASTICSEARCH_SERVICE_NAME}_SERVICE_PORT
    ELASTICSEARCH_URL=http://${!SVC_HOST}:${!SVC_PORT}
fi

# Wait for ES to start up properly
until $(curl -s -f -o /dev/null --connect-timeout 1 -m 1 --head ${ELASTICSEARCH_URL}); do
    sleep 0.1;
done

if ! [ $(curl -s -f -o /dev/null ${ELASTICSEARCH_URL}/.kibana) ]; then
    #curl -s -f -XPUT -d@/kibana-template.json "${ELASTICSEARCH_URL}/_template/kibana"

    type="_template"
    typefolder="kibana-objects/${type}"

    echo "Processing type $type"
    for fullfile in $typefolder/*.json; do
      filename=$(basename "$fullfile")
      name="${filename%.*}"

      if [ "$name" != "*" ]; then
        echo "Processing file $fullfile with name: $name"
        curl -vvv -H "Content-Type: application/json" -s -f -XPUT -d@/${fullfile} "${ELASTICSEARCH_URL}/${type}/${name}"
      fi
    done

    declare -a arr=("index-pattern" "search" "visualization" "dashboard" "config")

    for type in "${arr[@]}"
    do
      typefolder="kibana-objects/${type}"

      echo "Processing type $type"
      for fullfile in $typefolder/*.json; do
        filename=$(basename "$fullfile")
        name="${filename%.*}"

        if [ "$name" != "*" ]; then
          echo "Processing file $fullfile with name: $name"
          #curl -vvv -H "Content-Type: application/json" -s -f -XPUT -d@/${name}.json "${ELASTICSEARCH_URL}/.kibana/${type}/${name}"
          curl -vvv -H "Content-Type: application/json" -s -f -XPUT -d@/${fullfile} "${ELASTICSEARCH_URL}/.kibana/${type}/${name}"
        fi
      done
    done
fi

sleep infinity

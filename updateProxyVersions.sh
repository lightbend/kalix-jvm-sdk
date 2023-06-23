#!/usr/bin/env bash

# USAGE:
# > PROXY_VERSION=1.0.31 ./updateProxyVersions.sh

# this script is meant to be used after a new Proxy version is out
# to facilitate the update of all the places where we usually depend on the latest version

# provide the new proxy version you want the project to be updated to
if [[ -z "$PROXY_VERSION" ]]; then
    echo "Must provide PROXY_VERSION in environment" 1>&2
    exit 1
fi

echo ">>> Updating docker image versions to $PROXY_VERSION"
PROJS=$(find . -type f -name "docker-compose*.yml")
for i in ${PROJS[@]}
do
  echo "Updating Dockerfile for: $i"
  sed -i.bak "s/gcr.io\/kalix-public\/kalix-proxy:\(.*\)/gcr.io\/kalix-public\/kalix-proxy:$PROXY_VERSION/" $i
  rm $i.bak
done

echo ">>> Updating Dependencies.scala"
sed -i.bak "s/System.getProperty(\"kalix-proxy.version\", \"\(.*\)\")/System.getProperty(\"kalix-proxy.version\", \"$PROXY_VERSION\")/" ./project/Dependencies.scala
rm ./project/Dependencies.scala.bak

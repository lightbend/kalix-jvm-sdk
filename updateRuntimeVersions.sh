#!/usr/bin/env bash

# USAGE:
# > RUNTIME_VERSION=1.0.31 ./updateRuntimeVersions.sh

# this script is meant to be used after a new Kalix Runtime version is out
# to facilitate the update of all the places where we usually depend on the latest version

# provide the new Kalix Runtime version you want the project to be updated to
if [[ -z "$RUNTIME_VERSION" ]]; then
    echo "Must provide RUNTIME_VERSION in environment" 1>&2
    exit 1
fi

echo ">>> Updating docker image versions to $RUNTIME_VERSION"
PROJS=$(find . -type f -name "docker-compose*.yml")
for i in ${PROJS[@]}
do
  echo "Updating Dockerfile for: $i"
  sed -i.bak "s/gcr.io\/kalix-public\/kalix-runtime:\(.*\)/gcr.io\/kalix-public\/kalix-runtime:$RUNTIME_VERSION/" $i
  rm $i.bak
done

echo ">>> Updating Dependencies.scala"
sed -i.bak "s/System.getProperty(\"kalix-proxy.version\", \"\(.*\)\")/System.getProperty(\"kalix-proxy.version\", \"$RUNTIME_VERSION\")/" ./project/Dependencies.scala
rm ./project/Dependencies.scala.bak

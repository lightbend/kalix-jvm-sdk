#!/bin/bash

# this script is meant to be used after a new SDK version is out
# to facilitate the update of all the places where we usually depend on the latest version

# provide the new sdk version you want the project to be updated to
if [[ -z "$SDK_VERSION" ]]; then
    echo "Must provide SDK_VERSION in environment" 1>&2
    exit 1
fi

echo ">>> Updating pom versions to $SDK_VERSION"
find ./samples -type f -name "*pom.xml" -print0 | xargs -0 sed  -i '' -e "s/<kalix-sdk.version>\(.*\)<\/kalix-sdk.version>/<kalix-sdk.version>$SDK_VERSION<\/kalix-sdk.version>/"

echo ">>> Updating sbt plugins to $SDK_VERSION"
find ./samples -type f -name "*plugins.sbt" -print0 | xargs -0 sed -i '' -e "s/System.getProperty(\"kalix-sdk.version\", \".*\"))/System.getProperty(\"kalix-sdk.version\", \"$SDK_VERSION\"))/"

echo ">>> Updating maven plugin to $SDK_VERSION"
cd maven-java && mvn versions:set -DnewVersion="$SDK_VERSION"

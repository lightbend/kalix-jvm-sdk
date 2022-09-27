#!/bin/bash

if [ -z ${SDK_VERSION+x} ]; then
  SDK_VERSION=$(sbt "print sdkJava/version" | tail -1)
fi

echo ">>> Updating pom versions"
./updatePomVersions.sh

echo ">>> Updating sbt plugins"
find ./samples -type f -name "*plugins.sbt" -print0 | xargs -0 sed -i '' -e "s/System.getProperty(\"kalix-sdk.version\", \".*\"))/System.getProperty(\"kalix-sdk.version\", \"$SDK_VERSION\"))/"

echo ">>> Updating maven plugin"
cd maven-java && mvn versions:set -DnewVersion="$SDK_VERSION"

#gh pr create -B main -t "Testing Auto PR - Update JVM SDK version to $SDK_VERSION" -b "Please review and merge if appropriate." || echo "No changes"; exit 0


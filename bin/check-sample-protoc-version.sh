#!/usr/bin/env bash
set -euo pipefail

AKKA_GRPC_PROTOC_FULL=$(sbt -no-colors "print coreSdk/protocVersion" 1>/tmp/kalix-version.txt 2>/dev/null)
# the tail distance is different locally and on CI
AKKA_GRPC_PROTOC=$(cat /tmp/kalix-version.txt | tail -n 1 | head -n 1 | xargs)
POM_PROTOC_VERSIONS=$(find ./samples ./maven-java/ -name pom.xml | xargs egrep -h "com.google.protobuf:protoc:" | sed 's/.*:protoc://' | sed 's/:exe.*//' | sort | uniq)
if [ $(echo "$POM_PROTOC_VERSIONS" | wc -l) -ne 1 ]; then
  echo  "Not all Java Samples an Archetypes have the same protoc version:"
  find ./samples ./maven-java/ -name pom.xml | xargs egrep "com.google.protobuf:protoc:"
  false
elif [ "$AKKA_GRPC_PROTOC" != "$POM_PROTOC_VERSIONS" ]; then
  echo  "Java Samples and Archetypes ($POM_PROTOC_VERSIONS) does not have the same protoc version as Akka gRPC ($AKKA_GRPC_PROTOC)"
  echo "DEBUG: POM_PROTOC_VERSIONS=\"\"\"$POM_PROTOC_VERSIONS\"\"\""
  echo "DEBUG: AKKA_GRPC_PROTOC=\"\"\"$AKKA_GRPC_PROTOC\"\"\""
  echo "DEBUG: full Akka gRPC version text: \"\"\"$(cat /tmp/kalix-version.txt)\"\"\""
  false
fi

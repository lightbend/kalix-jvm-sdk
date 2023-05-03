#!/usr/bin/env bash
set -euo pipefail

AKKA_GRPC_PROTOC_FULL=$(sbt "print coreSdk/protocVersion" 2>/dev/null | tail -n 2)
echo "full Akka gRPC version text: \"\"\"${AKKA_GRPC_PROTOC_FULL}\"\"\""
AKKA_GRPC_PROTOC=$(echo $AKKA_GRPC_PROTOC_FULL | head -n 1)
POM_PROTOC_VERSIONS=$(find ./samples ./maven-java/ -name pom.xml | xargs egrep -h "com.google.protobuf:protoc:" | sed 's/.*:protoc://' | sed 's/:exe.*//' | sort | uniq)
if [ $(echo "$POM_PROTOC_VERSIONS" | wc -l) -ne 1 ]; then
  echo  "Not all Java Samples an Archetypes have the same protoc version:"
  find ./samples ./maven-java/ -name pom.xml | xargs egrep "com.google.protobuf:protoc:"
  false
elif [ "$AKKA_GRPC_PROTOC" != "$POM_PROTOC_VERSIONS" ]; then
  echo  "Java Samples and Archetypes ($POM_PROTOC_VERSIONS) does not have the same protoc version as Akka gRPC ($AKKA_GRPC_PROTOC)"
  false
fi

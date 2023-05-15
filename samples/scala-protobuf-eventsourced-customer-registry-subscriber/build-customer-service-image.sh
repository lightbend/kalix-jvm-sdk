#!/usr/bin/env bash

# this is script is used in CI to build scala-protobuf-eventsourced-customer-registry image
# in CI we'll have a file kalix-sdk-version.txt at the home dir

export SDK_VERSION=$(cat ~/kalix-sdk-version.txt)
cd ../scala-protobuf-eventsourced-customer-registry 
sbt -Dkalix-sdk.version=${SDK_VERSION} docker:publishLocal
cd -

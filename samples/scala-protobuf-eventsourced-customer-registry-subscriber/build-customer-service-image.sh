#!/usr/bin/env bash

# This is script is used in CI to build scala-protobuf-eventsourced-customer-registry image.
#
# The CI script uses a matrix with a pre-cmd hook to run tasks before the tests.
# However, we can't pass more than one command in the pre-cmd hook. 
# To work it around, we pass a simple shell script with all the steps that we require for the pre-cmd hook.

# in CI we'll have a file kalix-sdk-version.txt at the home dir
export SDK_VERSION=$(cat ~/kalix-sdk-version.txt)
cd ../scala-protobuf-eventsourced-customer-registry 
sbt -Dkalix-sdk.version=${SDK_VERSION} \
  -Ddocker.registry='kcr.us-east-1.kalix.io' -Ddocker.username=acme \
  docker:publishLocal
cd -

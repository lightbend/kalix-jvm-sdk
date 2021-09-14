#!/bin/bash

set -e

ONLY_MAVEN=0

for arg in "$@"; do case $arg in
  -o|--only-maven)
    ONLY_MAVEN=1
    shift
    ;;
  esac
done

if [ $ONLY_MAVEN == "0" ]; then
  sbt publishM2
fi

SDK_VERSION=$(sbt "print sdkJava/version" | tail -1)
cd maven-java
mvn versions:set -DnewVersion=$SDK_VERSION
mvn install

# cleanup
rm pom.xml.versionsBackup
rm */pom.xml.versionsBackup

git checkout pom.xml
git checkout */pom.xml

# tell the user what to do next
echo now you execute: export SDK_VERSION=$SDK_VERSION

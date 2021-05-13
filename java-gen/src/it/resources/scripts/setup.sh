#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Install our codegen jar into local maven Repo
mvn install:install-file \
   -Dfile=./akkasls-codegen-java_2.13-1.0-SNAPSHOT.jar \
   -DgroupId=com.lightbend \
   -DartifactId=akkasls-codegen-java_2.13 \
   -Dversion=1.0-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true

# Set up and install our maven-java tooling
pushd akkaserverless-maven-java

# Update the plugin to use the snapshot JAR
sed -i \
    '/<artifactId>akkasls-codegen-java_2.13<\/artifactId>/!b;n;c <version>1.0-SNAPSHOT</version>' \
    akkasls-maven-plugin/pom.xml

mvn install

popd

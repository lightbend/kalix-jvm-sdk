#!/usr/bin/env bash

echo "Extracting the SDK version from sbt build"
SDK_VERSION=$(sbt "print coreSdk/version" | tail -n 1)
IS_SNAPSHOT=$(sbt "print coreSdk/isSnapshot"  | tail -n 1)
if [ "true" == "${IS_SNAPSHOT}" ]
then
  REPO="akka-repo::default::https://maven.cloudsmith.io/lightbend/akka-snapshots/"
else
  REPO="akka-repo::default::https://maven.cloudsmith.io/lightbend/akka/"
fi
echo "Publishing ${SDK_VERSION} to ${REPO}"
echo ${PGP_SECRET} | base64 -d | gpg --import --batch
mkdir -p ~/.m2
cat >~/.m2/settings.xml \<<EOF;
<settings xmlns="http://maven.apache.org/SETTINGS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd">
  <servers>
    <server>
      <id>akka-repo</id>
      <username>${PUBLISH_USER}</username>
      <password>${PUBLISH_PASSWORD}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>release</id>
      <properties>
        <gpg.passphrase>${PGP_PASSPHRASE}</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
EOF
mvn -B versions:set -DnewVersion=${SDK_VERSION}
mvn -P release -B deploy -DaltDeploymentRepository="${REPO}"

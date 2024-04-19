#!/usr/bin/env bash

echo "Extracting the SDK version from sbt build"

# debugging help
echo "----"
sbt --client --no-colors "print coreSdk/version"
echo "----"
SDK_VERSION=$(sbt --client --no-colors "print coreSdk/version" | tail -n 3 | head -n 1 | tr -d '\n')
echo "Publishing '${SDK_VERSION}'"

cd maven-java

# update poms with the version extracted from sbt dynver (-B == no colors)
mvn -B versions:set -DnewVersion=${SDK_VERSION}

# create Maven settings.xml with credentials for repository publishing
mkdir -p ~/.m2
cat <<EOF >~/.m2/settings.xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd">
  <servers>
      <server>
        <id>ossrh</id>
        <username>${SONATYPE_USERNAME}</username>
        <password>${SONATYPE_PASSWORD}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.passphrase>${PGP_PASSPHRASE}</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
EOF

# import the artefact signing key
echo ${PGP_SECRET} | base64 -d | gpg --import --batch

# Maven deply with profile `release` (-B == no colors)
mvn -P release -B deploy

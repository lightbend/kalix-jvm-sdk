#!/usr/bin/env bash
set -e

if [ -z "${SDK_VERSION}" ];
then
  echo "expected SDK_VERSION to be set"
  exit 1
fi
if [ -z "${SONATYPE_USERNAME}" ];
then
  echo "expected SONATYPE_USERNAME to be set"
  exit 1
fi
if [ -z "${SONATYPE_PASSWORD}" ];
then
  echo "expected SONATYPE_PASSWORD to be set"
  exit 1
fi
if [ -z "${PGP_PASSPHRASE}" ];
then
  echo "expected PGP_PASSPHRASE to be set"
  exit 1
fi
if [ -z "${PGP_SECRET}" ];
then
  echo "expected PGP_SECRET to be set"
  exit 1
fi

cd maven-java

# update poms with the version extracted from sbt dynver
mvn --quiet --batch-mode versions:set -DnewVersion=${SDK_VERSION}

  ( # also needs to change kalix-sdk.version in parent pom
    cd kalix-java-protobuf-parent
    sed -i.bak "s/<kalix-sdk.version>\(.*\)<\/kalix-sdk.version>/<kalix-sdk.version>$SDK_VERSION<\/kalix-sdk.version>/" pom.xml
  )

  ( # also needs to change kalix-sdk.version in parent pom
    cd kalix-spring-boot-parent
    sed -i.bak "s/<kalix-sdk.version>\(.*\)<\/kalix-sdk.version>/<kalix-sdk.version>$SDK_VERSION<\/kalix-sdk.version>/" pom.xml
  )


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
echo "${PGP_SECRET}" | base64 -d | gpg --import --batch

# Maven deploy with profile `release`
# mvn --quiet --batch-mode --activate-profiles release deploy
mvn --batch-mode --activate-profiles release deploy

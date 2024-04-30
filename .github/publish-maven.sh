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
../.github/patch-maven-versions.sh

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
        <altSnapshotDeploymentRepository>snapshots::default::https://s01.oss.sonatype.org/content/repositories/snapshots</altSnapshotDeploymentRepository>
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
mvn --batch-mode --activate-profiles release -Dskip.docker=true deploy

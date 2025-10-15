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

# create or merge Maven settings.xml with credentials for repository publishing
MVN_SETTINGS_FILE="$HOME/.m2/settings.xml"
mkdir -p "$(dirname "$MVN_SETTINGS_FILE")"

if [ -f "$MVN_SETTINGS_FILE" ]; then
    echo "⚙️ Settings file found. Merging publishing configuration..."
    xmlstarlet ed -L \
        -s /settings -t elem -n servers -v "" \
        -s /settings/servers[1] -t elem -n server \
        -i /settings/servers[1]/server[1] -t attr -n id -v central \
        -s /settings/servers[1]/server[1] -t elem -n username -v '${SONATYPE_USERNAME}' \
        -s /settings/servers[1]/server[1] -t elem -n password -v '${SONATYPE_PASSWORD}' \
        "$MVN_SETTINGS_FILE"
    xmlstarlet ed -L \
        -s '/settings/profiles/profile[id="akka-repo"]' -t elem -n activation -v "" \
        -s '/settings/profiles/profile[id="akka-repo"]/activation' -t elem -n activeByDefault -v "true" \
        -s '/settings/profiles/profile[id="akka-repo"]' -t elem -n properties -v "" \
        -s '/settings/profiles/profile[id="akka-repo"]/properties' -t elem -n gpg.passphrase -v '${PGP_PASSPHRASE}' \
        "$MVN_SETTINGS_FILE"
    echo "✅ Merge complete."
else
    echo "✨ Settings file not found. Creating a new one with publishing configuration."
    cat <<EOF >"$MVN_SETTINGS_FILE"
<settings xmlns="http://maven.apache.org/SETTINGS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd">
  <servers>
      <server>
        <id>central</id>
        <username>${SONATYPE_USERNAME}</username>
        <password>${SONATYPE_PASSWORD}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>maven-central-publishing</id>
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
    echo "✅ File created."
fi

# import the artefact signing key
echo "${PGP_SECRET}" | base64 -d | gpg --import --batch

# Maven deploy with profile `release`
# mvn --quiet --batch-mode --activate-profiles release deploy
mvn --batch-mode --activate-profiles release -Dskip.docker=true deploy

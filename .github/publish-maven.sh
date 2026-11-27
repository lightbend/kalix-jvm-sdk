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
    echo "✅ Settings file already exists. It is expected to be set up with Sonatype publishing details already, so doing nothing to the file."
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

# Maven deploy with profile `release`. Only builds the bundle, uploading is done below
# since newer Maven versions add files to the bundle that Central rejects.
mvn --batch-mode --activate-profiles release -Dskip.docker=true -DskipPublishing=true deploy

BUNDLE="target/central-publishing/central-bundle.zip"
if [ ! -f "$BUNDLE" ]; then
  echo "expected bundle $BUNDLE to exist"
  exit 1
fi

# Local repository bookkeeping files, they have no .pom next to them
zip -d "$BUNDLE" '*maven-metadata-local.xml*' '*_remote.repositories*' || [ $? -eq 12 ]

AUTH="$(printf '%s:%s' "${SONATYPE_USERNAME}" "${SONATYPE_PASSWORD}" | base64 -w0)"
CENTRAL_API="https://central.sonatype.com/api/v1/publisher"

DEPLOYMENT_ID="$(curl --fail --silent --show-error \
  --header "Authorization: Bearer ${AUTH}" \
  --form "bundle=@${BUNDLE}" \
  "${CENTRAL_API}/upload?name=kalix-maven-${SDK_VERSION}&publishingType=AUTOMATIC")"
echo "Uploaded bundle, deployment id: ${DEPLOYMENT_ID}"

# Wait for validation and publishing
for _ in $(seq 1 60); do
  STATE="$(curl --fail --silent --show-error --request POST \
    --header "Authorization: Bearer ${AUTH}" \
    "${CENTRAL_API}/status?id=${DEPLOYMENT_ID}" | sed -n 's/.*"deploymentState":"\([A-Z]*\)".*/\1/p')"
  echo "Deployment state: ${STATE}"
  case "$STATE" in
    PUBLISHING|PUBLISHED) exit 0 ;;
    FAILED) echo "Deployment ${DEPLOYMENT_ID} failed"; exit 1 ;;
  esac
  sleep 10
done
echo "Timed out waiting for deployment ${DEPLOYMENT_ID}"
exit 1

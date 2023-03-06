# This script will publish the current snapshot of all artifacts. 
# Including the maven plugin and archetypes.

SDK_VERSION=$(sbt "print javaSdkProtobuf/version" | tail -1)

echo
echo "------------------------------------------------------------------------"
echo "Publishing version $SDK_VERSION"
echo "------------------------------------------------------------------------"

sbt 'publishM2; publishLocal'
(
  cd maven-java
  mvn versions:set -DnewVersion=$SDK_VERSION
  mvn clean install

  # cleanup
  rm pom.xml.versionsBackup
  rm */pom.xml.versionsBackup

  # revert
  git checkout pom.xml
  git checkout */pom.xml
)


echo $SDK_VERSION

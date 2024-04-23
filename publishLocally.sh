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

  (
    # special case for parent pom, version:set only changes project sharing the same parent
    # kalix-spring-boot-parent is special because its parent is spring-boot-starter-parent
    cd kalix-spring-boot-parent
    mvn versions:set -DnewVersion=$SDK_VERSION

    # also needs to change kalix-sdk.version in parent pom
    sed "s/<kalix-sdk.version>\(.*\)<\/kalix-sdk.version>/<kalix-sdk.version>$SDK_VERSION<\/kalix-sdk.version>/" pom.xml
  )

  mvn clean install

  # cleanup
  rm pom.xml.versionsBackup
  rm */pom.xml.versionsBackup

  # revert
  git checkout pom.xml
  git checkout */pom.xml
)


echo $SDK_VERSION

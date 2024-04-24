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
  mvn --activate-profiles patch-version versions:set -DnewVersion=$SDK_VERSION
  
  ( # also needs to change kalix-sdk.version in parent pom
    cd kalix-java-protobuf-parent
    sed -i.bak "s/<kalix-sdk.version>\(.*\)<\/kalix-sdk.version>/<kalix-sdk.version>$SDK_VERSION<\/kalix-sdk.version>/" pom.xml
    rm pom.xml.bak
  )

  ( # also needs to change kalix-sdk.version in parent pom
    cd kalix-spring-boot-parent
    sed -i.bak "s/<kalix-sdk.version>\(.*\)<\/kalix-sdk.version>/<kalix-sdk.version>$SDK_VERSION<\/kalix-sdk.version>/" pom.xml
    rm pom.xml.bak
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

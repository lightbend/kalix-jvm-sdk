
# this script updates all maven projects versions (maven-java and samples) to align with the current sdk version
# if SDK_VERSION env var is defined, it will use it, otherwise it will take the version from sbt
# after running this script, you may run local tests or simply send a PR with the updates. 

# you can pass the path to a sample to only change the version for single sample, 
# eg: ./updatePomVersions.sh samples/java-eventsourced-counter
# or you can pass 'all' to it and all java samples (pom.xml) will be updated
# eg: ./updatePomVersions.sh all
# useful when testing out new functionality locally.


if [ -z ${SDK_VERSION+x} ]; then 
  SDK_VERSION=$(sbt "print sdkJava/version" | tail -1)
fi

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

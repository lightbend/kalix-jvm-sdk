# this script updates all maven projects versions (maven-java and samples) to align with the current sdk version
# if SDK_VERSION env var is defined, it will use it, otherwise it will take the version from sbt
# after running this script, you may run local tests or simply send a PR with the updates. 
# the script can also run publishM2 and mvn install to generate the artifacts with the new version.


if [ -z ${SDK_VERSION+x} ]; then 
  SDK_VERSION=$(sbt "print sdk/version" | tail -1)
fi

if [ $1 ]; then 
  sbt publishM2
fi 

(
  cd maven-java
  mvn versions:set -DnewVersion=$SDK_VERSION

  if [ $1 ]; then 
    mvn install
  fi

  # cleanup
  rm pom.xml.versionsBackup
  rm */pom.xml.versionsBackup
)

for i in samples/*
do
  (
    cd $i
    mvn versions:set -DnewVersion=$SDK_VERSION
    rm pom.xml.versionsBackup
  )
done
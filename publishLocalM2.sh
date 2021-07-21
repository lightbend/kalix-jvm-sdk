# publish sbt and maven artifacts
sbt publishM2
SDK_VERSION=$(sbt "print sdk/version" | tail -1)
cd maven-java
mvn versions:set -DnewVersion=$SDK_VERSION
mvn install

# cleanup
rm pom.xml.versionsBackup
rm */pom.xml.versionsBackup

git checkout pom.xml
git checkout */pom.xml

# tell the user what to do next
echo now you execute: export SDK_VERSION=$SDK_VERSION

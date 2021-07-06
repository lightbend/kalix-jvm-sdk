# Contributing to AkkaSerrverless Java SDK

FIXME contribution guidelines like in other LB projects


# Project tips

##  Trying changes to the codegen out with the samples

1. Publish the SDK artifacts to the local maven repo `sbt publishM2`, copy the released snapshot version from the output.
2. Set the maven plugin version to the version sbt generated:

```shell
cd maven-java
mvn versions:set -DnewVersion="0.7.0-beta.10-1-370-413a4e8e-SNAPSHOT"
mvn install
```

3. Pass that version to the sample projects when building:

```shell
cd samples/java-valueentity-shopping-cart
mvn -Dakkaserverless-maven-plugin.version="0.7.0-beta.10-1-370-413a4e8e-SNAPSHOT" compile
```

Be careful not to accidentally check in the `maven-java` `pom.xml` files with changed version.
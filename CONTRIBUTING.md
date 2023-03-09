# Contributing to Kalix JVM SDKs

FIXME contribution guidelines like in other LB projects


# Project tips

##  Trying changes to the codegen out with the Java samples

1. Publish the SDK artifacts to the local maven repo 
    ```shell
    sbt
    set publishArtifact in (Compile, packageDoc) in ThisBuild := false
    publishM2
    ```
   * copy the released snapshot version from the output and use it in next steps
   * the `set publishArtifact` speed up packaging faster by skipping doc generation
   * `publishM2` is needed when working with Java samples

2. Set the maven plugin version to the version sbt generated:

    ```shell
    cd maven-java
    mvn versions:set -DnewVersion="0.7...-SNAPSHOT"
    mvn install
    git checkout .
    ```

3. Pass that version to the sample projects when building:

    ```shell
    cd samples/java-protobuf-valueentity-shopping-cart
    mvn -Dkalix-sdk.version="0.7...-SNAPSHOT" compile
    ```

Be careful not to accidentally check in the `maven-java` `pom.xml` files with changed version.

Ensure to remove/update generated files under `src` if they cause problems.

##  Trying changes to the codegen out with the Scala samples

1. Publish the SDK artifacts to the local maven repo
    ```shell
    sbt
    set publishArtifact in (Compile, packageDoc) in ThisBuild := false
    publishLocal
    ```
   * copy the released snapshot version from the output and use it in next steps
   * the `set publishArtifact` speed up packaging faster by skipping doc generation
   * `publishLocal` is needed when working with Scala samples
   
2. Pass that version to the sample projects when building:

    ```shell
    cd samples/scala-protobuf-valueentity-customer-registry
    sbt -Dkalix-sdk.version="0.7...-SNAPSHOT" compile
    ```

Ensure to remove/update generated files under `src` if they cause problems.

## Interactively run sbt scripted tests

There are a number of Scala projects being tested as 'scripted' tests. After a
scripted test has completed (successfully or not) it removes the temporary
directory where it was running.

To interactively inspect the project, you can insert a `$ pause` before the
first failing command in the `test` script of a scripted test. You can then
inspect the project, and enter it interactively with 
`cd /tmp/sbt_431151; sbt -Dplugin.version=....`, specifying the snapshot
version of the project. Adding `> protocGenerate` or `> generateUnmanaged`
before `$ pause` might save entering sbt yourself a few times.

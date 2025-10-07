# Contributing to Kalix JVM SDKs

FIXME contribution guidelines like in other LB projects


# Project tips

## Build Token

To build locally, you need to fetch a token at https://account.akka.io/token that you have to place into `~/.sbt/1.0/akka-commercial.sbt` file like this:
```
ThisBuild / resolvers += "lightbend-akka".at("your token resolver here")
```

and for Maven you need to add the following int `~/.m2/settings.xml`:

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>akka-repo-redirect</id>
      <mirrorOf>akka-repository</mirrorOf>
      <url>your token resolver here</url>
    </mirror>
    <mirror>
      <mirrorOf>external:http:*</mirrorOf>
      <name>Pseudo repository to mirror external repositories initially using HTTP.</name>
      <url>http://0.0.0.0/</url>
      <blocked>true</blocked>
      <id>maven-default-http-blocker</id>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <id>akka-repo</id>
      <repositories>
        <repository>
          <id>akka-repository</id>
          <url>your token resolver here</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>akka-plugin-repository</id>
          <url>your token resolver here</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>akka-repo</activeProfile>
  </activeProfiles>
</settings>
```

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

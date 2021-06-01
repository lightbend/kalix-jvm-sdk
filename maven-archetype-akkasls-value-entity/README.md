# maven-archetype-akkasls-value-entity

![Test](https://github.com/lightbend/akkaserverless-maven-archetype/workflows/Test/badge.svg)

This archetype can be used to generate a project suitable for the general development of Value-based entities using [Akka Serverless](https://www.lightbend.com/akka-serverless).

**The Maven project created from the archetype will contain a Value entity protobuf definition.**

The archetype is located at Lightbend's Maven repository. Please ensure that
your Maven's `settings.xml` file points at `repo.lightbend.com`. Here are the repository
declarations you will require for your `settings.xml`:

> `settings.xml` is a file to be found in your home directory and then within a sub-directory named `.m2`

> Your organization may have configured a proxy to be used to download Maven artifacts.
> If you have difficulties updating your `settings.xml` then please consult with your
> systems administrator.

```xml
  <profiles>
    <profile>
      <id>lightbend</id>
      <repositories>
        <repository>
          <id>lightbend-akkaserverless</id>
          <name>repo-lightbend-com-akkaserverless</name>
          <url>https://repo.lightbend.com/lightbend/akkaserverless/</url>
          <releases>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
          </releases>
          <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
```

Also ensure that the profile is active within the `settings.xml`:

```xml
  <activeProfiles>
    <activeProfile>lightbend</activeProfile>
  </activeProfiles>
```

A complete settings.xml should look like:
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <profiles>
        <profile>
            <id>lightbend</id>
            <repositories>
                <repository>
                    <id>lightbend-akkaserverless</id>
                    <name>repo-lightbend-com-akkaserverless</name>
                    <url>https://repo.lightbend.com/lightbend/akkaserverless/</url>
                    <releases>
                        <enabled>true</enabled>
                        <updatePolicy>always</updatePolicy>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                        <updatePolicy>always</updatePolicy>
                    </snapshots>
                </repository>
            </repositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>lightbend</activeProfile>
    </activeProfiles>
</settings>
```

For the latest release see [GitHub releases](https://github.com/lightbend/akkaserverless-maven-java/releases).

Then, to use on Linux and macOS:

```
mvn \
  archetype:generate \
  -DarchetypeGroupId=com.lightbend \
  -DarchetypeArtifactId=maven-archetype-akkasls \
  -DarchetypeVersion=LATEST
```

To use on Windows 10 or later:

```
mvn ^
  archetype:generate ^
  -DarchetypeGroupId=com.lightbend ^
  -DarchetypeArtifactId=maven-archetype-akkasls ^
  -DarchetypeVersion=LATEST
```

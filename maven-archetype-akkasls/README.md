# maven-archetype-akkasls

![Test](https://github.com/lightbend/akkaserverless-maven-archetype/workflows/Test/badge.svg)

This archetype can be used to generate a project suitable for the general development of
[event-sourced](https://martinfowler.com/eaaDev/EventSourcing.html) and Value-based entities using [Akka Serverless](https://www.lightbend.com/akka-serverless).

The archetype is located at our Cloudsmith repo. Please ensure that
your Maven's `settings.xml` file points at Cloudsmith. Here are the repository
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
          <url>https://dl.cloudsmith.io/public/lightbend/akkaserverless/maven/</url>
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

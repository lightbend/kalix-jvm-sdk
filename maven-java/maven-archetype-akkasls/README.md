# maven-archetype-akkasls

![Test](https://github.com/lightbend/akkaserverless-maven-archetype/workflows/Test/badge.svg)

This archetype can be used to generate a project suitable for the general development of 
[event-sourced](https://martinfowler.com/eaaDev/EventSourcing.html) 
[Akka Serverless](https://www.lightbend.com/akka-serverless) applications.

To use on Linux and macOS, noting the `CHANGEME=TO-VERSION-TO-USE`:

```
curl \
  "https://bintray.com/repo/downloadMavenRepoSettingsFile/downloadSettings?repoPath=%2Flightbend%2Fakkaserverless" > \
   /tmp/maven-archetype-akkasls-settings.xml && \
mvn \
  --settings /tmp/maven-archetype-akkasls-settings.xml \
  archetype:generate \
  -DarchetypeGroupId=com.lightbend \
  -DarchetypeArtifactId=maven-archetype-akkasls \
  -DarchetypeVersion=<CHANGEME=TO-VERSION-TO-USE>
```

To use on Windows 10 or later, also noting the `CHANGEME=TO-VERSION-TO-USE`:

```
curl ^
  "https://bintray.com/repo/downloadMavenRepoSettingsFile/downloadSettings?repoPath=%2Flightbend%2Fakkaserverless" ^
   > %Temp%\maven-archetype-akkasls-settings.xml && ^
mvn ^
  --settings %Temp%\maven-archetype-akkasls-settings.xml ^
  archetype:generate ^
  -DarchetypeGroupId=com.lightbend ^
  -DarchetypeArtifactId=maven-archetype-akkasls ^
  -DarchetypeVersion=<CHANGEME=TO-VERSION-TO-USE>
```

To use on other operating systems, in essence download the above `maven-archetype-akkasls-settings.xml` to a temporary
location and use the `--settings` option with `mvn` to point at it.
# akkaserverless-maven-archetype-event-sourced-entity

This archetype can be used to generate a project suitable for the general development of
[event-sourced](https://martinfowler.com/eaaDev/EventSourcing.html) using [Akka Serverless](https://www.lightbend.com/akka-serverless).

**The Maven project created from the archetype will contain an Event Sourced entity protobuf definition.**

For the latest release see [GitHub releases](https://github.com/lightbend/akkaserverless-java-sdk/releases).

To kickstart a project on Linux and macOS:

```
mvn \
  archetype:generate \
  -DarchetypeGroupId=com.akkaserverless \
  -DarchetypeArtifactId=akkaserverless-maven-archetype-event-sourced-entity \
  -DarchetypeVersion=LATEST
```

To kickstart a project on Windows 10 or later:

```
mvn ^
  archetype:generate ^
  -DarchetypeGroupId=com.akkaserverless ^
  -DarchetypeArtifactId=akkaserverless-maven-archetype-event-sourced-entity ^
  -DarchetypeVersion=LATEST
```

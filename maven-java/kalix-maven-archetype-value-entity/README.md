# kalix-maven-archetype

This archetype can be used to generate a project suitable for the development of a Service using a Value Entity with [Kalix](https://docs.kalix.io).

**The Maven project created from the archetype will contain a Value entity protobuf definition.**

For the latest release see [GitHub releases](https://github.com/lightbend/kalix-jvm-sdk/releases).

To kickstart a project on Linux and macOS:

```shell
mvn \
  archetype:generate \
  -DarchetypeGroupId=io.kalix \
  -DarchetypeArtifactId=kalix-maven-archetype \
  -DarchetypeVersion=LATEST
```

To kickstart a project on Windows 10 or later:

```shell
mvn ^
  archetype:generate ^
  -DarchetypeGroupId=io.kalix ^
  -DarchetypeArtifactId=kalix-maven-archetype ^
  -DarchetypeVersion=LATEST
```

# kalix-spring-boot-archetype

This archetype can be used to generate a project suitable for the development of a Service using [Kalix](https://docs.kalix.io).

For the latest release see [GitHub releases](https://github.com/lightbend/kalix-jvm-sdk/releases).

To kickstart a project on Linux and macOS:

```shell
mvn \
  archetype:generate \
  -DarchetypeGroupId=io.kalix \
  -DarchetypeArtifactId=kalix-spring-boot-archetype \
  -DarchetypeVersion=LATEST
```

To kickstart a project on Windows 10 or later:

```shell
mvn ^
  archetype:generate ^
  -DarchetypeGroupId=io.kalix ^
  -DarchetypeArtifactId=kalix-spring-boot-archetype ^
  -DarchetypeVersion=LATEST
```

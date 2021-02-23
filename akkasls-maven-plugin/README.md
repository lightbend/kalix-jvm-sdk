# akkasls-maven-plugin

![Test](https://github.com/lightbend/akkaserverless-maven-plugin/workflows/Test/badge.svg)

This plugin is designed for developers of Maven and Java projects that wish to target the
[Akka Serverless](https://www.lightbend.com/akka-serverless) environment.

The plugin provides two functions:

* generate templated source code from entity, command, event and state information declared by .`proto` files; and
* the ability to deploy directly to Akka Serverless.

## Usage

To use the plugin (substitute the version accordingly):

```xml
<plugin>
<groupId>com.lightbend</groupId>
<artifactId>akkasls-maven-plugin</artifactId>
<version>1.0-SNAPSHOT</version>
<executions>
  <execution>
    <goals>
      <goal>generate</goal>
      <goal>deploy</goal>
    </goals>
  </execution>
</executions>
</plugin>
```

By default, the plugin will search for protobuf service declarations that are suffixed with `ServiceEntity`.
This can be overridden by declaring a regex pattern for the plugin's `serviceNamesFilter` configuration e.g. to 
search for services ending in `Service` instead of the default ending of `ServiceEntity`:

```xml
<serviceNamesFilter>.*Service</serviceNamesFilter>
```

This same setting can be used to be explicit about what services relate to entities. Conversely, specifying
`.*` permits all services to become candidate entities. In future, we expect that protobuf options will be introduced
such that entities can be easily identified. At that time, the `.*` value will become appropriate.

## Configuration

- `akkaslsPath` path and name of the `akkasls` commmand line tool
- `akkaslsContext` selects the context when calling `akkasls` if set
- `dockerImage` the Docker image name (use eg. `<dockerImage>${akkasls.dockerImage}:${akkasls.dockerTag}</dockerImage>`)

## Building and testing

Test sources are required to be generated, particularly when run from within an IDE and attempting to run tests. A simple
`mvn test` from the command line will suffice to get testing working.

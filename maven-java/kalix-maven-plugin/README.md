# kalix-maven-plugin

This plugin is designed for developers of Maven and Java projects that wish to target the
[Kalix](https://www.lightbend.com/akka-serverless) environment.

The plugin provides two functions:

* generate templated source code from entity, command, event and state information declared by `.proto` files; and
* the ability to deploy directly to Kalix.

## Usage

To use the plugin (substitute the version accordingly):

```xml
<plugin>
<groupId>com.lightbend</groupId>
<artifactId>kalix-maven-plugin</artifactId>
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

The plugin will search for protobuf service declarations relating to entities via various proto options
that have been introduced.

## Configuration

- `akkaslsPath` path and name of the `akkasls` command line tool
- `akkaslsContext` selects the context when calling `akkasls` if set
- `dockerImage` the Docker image name (use eg. `<dockerImage>${akkasls.dockerImage}:${akkasls.dockerTag}</dockerImage>`)

## Building and testing

Test sources are required to be generated, particularly when run from within an IDE and attempting to run tests. A simple
`mvn test` from the command line will suffice to get testing working.

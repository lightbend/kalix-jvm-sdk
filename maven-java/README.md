# akkasls-maven-plugin

This plugin is designed for developers of Maven and Java projects that wish to target the
[Akka Serverless](https://www.lightbend.com/akka-serverless) environment.

The plugin provides two functions:

* generate templated source code from entity, command, event and state information declared by .`proto` files; and
* the ability to deploy directly to Akka Serverless without requiring the `akkasls` CLI.

By including this plugin, no tooling other than Maven and Java are required to support a full-featured development lifecycle.

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
    </goals>
  </execution>
</executions>
</plugin>
```
## Building and testing

Test sources are required to be generated, particularly when run from within an IDE and attempting to run tests. A simple
`mvn test` from the command line will suffice to get testing working.
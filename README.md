# Kalix Java and Scala SDK

For more information see the documentation for [implementing Kalix services in Java or Scala](https://docs.kalix.io/java/index.html).

## Parts of the Kalix Java/Scala SDK

* user-facing packages
    * `kalix-java-sdk` and `kalix-scala-sdk` The Java/Scala API to build services in Kalix (backed by an implementation in Scala based on Akka gRPC).

      See [Developing with Java or Scala](https://docs.kalix.io/java/index.html).

    * `kalix-java-sdk-testkit` and `kalix-scala-sdk-testkit` A library to implement integration tests for services with Junit 4 or 5, based on [TestContainers](https://www.testcontainers.org/). Also contains library parts of unit TestKit.

    * [samples](samples/) Small example services to illustrate Kalix features. The code provides snippets for the documentation.

* developer tooling
   * `codegen/core`, `codegen/java-gen`, `codegen/scala-gen` Tooling to generate code from Protobuf with annotations.
   * `maven-java` Maven tooling
     * `kalix-maven-plugin` Maven plugin to trigger code generation.
     * `kalix-maven-archetype-event-sourced-entity` Maven archetype to create a project with an Event-sourced Entity.
     * `kalix-maven-archetype-value-entity` Maven archetype to create a project with a Value Entity.
   * `sbt-plugin` sbt plugin for code generation.
   * `sbt new` (gitter8) templates:
     * [lightbend/kalix-value-entity.g8](https://github.com/lightbend/kalix-value-entity.g8) 

* `docs` The documentation feeding into [Developing with Java or Scala](https://docs.kalix.io/java/index.html).

* `tck` The Technology Compatibility Kit which ensures the Java SDK adheres to the Kalix protocol.

# License

The Kalix Java SDK is Open Source and available under the [Apache 2 License](LICENSE).


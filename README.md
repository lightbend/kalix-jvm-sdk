# Akka Serverless Java and Scala SDK

For more information see the documentation for [implementing Akka Serverless services in Java or Scala](https://developer.lightbend.com/docs/akka-serverless/java/index.html).

## Parts of the Akka Serverless Java/Scala SDK

* user-facing packages
    * `akkaserverless-java-sdk` and `akkaserverless-scala-sdk` The Java/Scala API to build services in Akka Serverless (backed by an implementation in Scala based on Akka gRPC).

      See [Developing with Java or Scala](https://developer.lightbend.com/docs/akka-serverless/java/index.html).

    * `akkaserverless-java-sdk-testkit` and `akkaserverless-scala-sdk-testkit` A library to implement integration tests for services with Junit 4 or 5, based on [TestContainers](https://www.testcontainers.org/). Also contains library parts of unit TestKit.

    * [samples](samples/) Small example services to illustrate Akka Serverless features. The code provides snippets for the documentation.

* developer tooling
   * `codegen/core`, `codegen/java-gen`, `codegen/scala-gen` Tooling to generate code from Protobuf with annotations.
   * `maven-java` Maven tooling
     * `akkaserverless-maven-plugin` Maven plugin to trigger code generation.
     * `akkaserverless-maven-archetype-event-sourced-entity` Maven archetype to create a project with an Event-sourced Entity.
     * `akkaserverless-maven-archetype-value-entity` Maven archetype to create a project with a Value Entity.
   * `sbt-plugin` sbt plugin for code generation.
   * `sbt new` (gitter8) templates:
     * [lightbend/akkaserverless-value-entity.g8](https://github.com/lightbend/akkaserverless-value-entity.g8) 

* `docs` The documentation feeding into [Developing with Java or Scala](https://developer.lightbend.com/docs/akka-serverless/java/index.html).

* `tck` The Technology Compatibility Kit which ensures the Java SDK adheres to the Akka Serverless protocol.

# License

The Akka Serverless Java SDK is Open Source and available under the [Apache 2 License](LICENSE).

# Akka Serverless Java SDK

For more information see the documentation for [implementing Akka Serverless services in Java](https://developer.lightbend.com/docs/akka-serverless/java/index.html).

## Parts of the Akka Serverless Java SDK

* user-facing packages
    * `sdk` The Java API to build services in Akka Serverless (backed by an implementation in Scala based on Akka gRPC).

      See [Developing with Java](https://developer.lightbend.com/docs/akka-serverless/java/index.html) in the Akka Serverless documentation and the [JavaDoc API](https://developer.lightbend.com/docs/akka-serverless/java/_attachments/api/index.html).

    * `testkit` A library to implement integration tests for services with Junit 4 or 5, based on [TestContainers](https://www.testcontainers.org/).

    * `samples` Small example services to illustrate Akka Serverless features. The code provides snippets for the documentation.

* developer tooling
   * `codegen/core`, `codegen/java-gen` Tooling to generate code from Protobuf with annotations.
   * `maven-java` Maven tooling
     * `akkaserverless-maven-plugin` Maven plugin to trigger code generation.
     * `akkaserverless-maven-archetype-event-sourced-entity` Maven archetype to create a project with an Event-sourced Entity.
     * `akkaserverless-maven-archetype-value-entity` Maven archetype to create a project with a Value Entity.

* `docs` The documentation feeding into [Developing with Java](https://developer.lightbend.com/docs/akka-serverless/java/index.html).

* `tck` The Technology Compatibility Kit which ensures the Java SDK adheres to the Akka Serverless protocol.

# License

The Akka Serverless Java SDK is Open Source and available under the [Apache 2 License](LICENSE).

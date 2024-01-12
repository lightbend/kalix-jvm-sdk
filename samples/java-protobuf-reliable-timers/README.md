# Implementing an Order Service as a Value Entity in combination with Timers

This project is based on the Kalix Maven archetype.

```shell
mvn archetype:generate \
  -DarchetypeGroupId=io.kalix \
  -DarchetypeArtifactId=kalix-maven-archetype \
  -DarchetypeVersion=LATEST
```

See the [Kickstart a Maven project](https://docs.kalix.io/java/kickstart.html) in the documentation for details.

## Building and running unit tests

To compile and test the code from the command line, use

```shell
mvn verify
```

## Running the integration tests

The integration test sources are in the `src/it/java` directory.

NOTE: As this is not part of the standard Maven setup, you might need to add it as test source directory in your IDE. (IntelliJ: right click on the directory -> "Mark directory as" | "Test sources root")

Maven is configured to run tests from it when using the `it` profile.

```shell
mvn verify -Pit
```

The integration test uses Docker via [TestContainers](https://www.testcontainers.org/) to set up the Kalix environment before calling the gRPC API.

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`.

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.actions.Order/PlaceOrder -d '{ "item":"Pizza Margherita", "quantity":1 }'

# confirm or cancel before it expires
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.actions.Order/Confirm -d '{ "number" : "returned-order-number" }'
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.actions.Order/Cancel -d '{ "number" : "returned-order-number" }'
```

Or, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```shell
grpcurl -plaintext -d '{ "item":"Pizza Margherita", "quantity":1 }' localhost:9000 com.example.actions.Order/PlaceOrder

# confirm or cancel before it expires
grpcurl -plaintext -d '{ "number" : "returned-order-number" }' localhost:9000 com.example.actions.Order/Confirm
grpcurl -plaintext -d '{ "number" : "returned-order-number" }' localhost:9000 com.example.actions.Order/Cancel
```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Install Kalix](https://docs.kalix.io/kalix/install-kalix.html)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a project and then deploy your service into the project either by using `mvn deploy kalix:deploy` which
will conveniently package, publish your docker image, and deploy your service to Kalix, or by first packaging and
publishing the docker image through `mvn deploy` and then deploying the image
through the `kalix` CLI.

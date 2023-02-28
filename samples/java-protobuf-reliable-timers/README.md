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

In order to run your application locally, you must run the Kalix proxy. The included `docker compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:exec
```

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

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
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a project and then deploy your service into the project either by using `mvn deploy` which
will also conveniently package and publish your docker image prior to deployment, or by first packaging and
publishing the docker image through `mvn clean package docker:push -DskipTests` and then deploying the image
through the `kalix` CLI.

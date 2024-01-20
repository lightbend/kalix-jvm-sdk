# Customer Registry

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/developing/development-process-proto.html) in the documentation.

## Developing

This project demonstrates the use of Value Entity and View components.
To understand more about these components, see [Developing services](https://docs.kalix.io/services/)
and in particular the [Java Protobuf SDK section](https://docs.kalix.io/java-protobuf/)

## Building

You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```shell
mvn compile
```

## Running Locally

When running this Kalix service locally, we need to have its companion Kalix Runtime, a Kafka broker and a Zookeeper running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start the Kalix service, the Kalix Runtime, a local Kafka broker and a Zookeeper using the included [docker-compose.yml](./docker-compose.yml) file.

It can make sense to delete the existing containers (if any exist) and start from scratch when starting up the Kafka container.

### Create topic for eventing out

Every time a customer receives a change, the service also persists that change to a topic in Kafka called `customer_changes`. You can see that this is set in customer_action.proto. In order for the changes to be written to this topic, you need to manually create the topic in Kafka. To do this, run the following command after the Docker container `java-protobuf-customer-registry-kafka-quickstart-kafka-1` is running.

```shell
docker-compose exec kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic customer_changes
```

### Exercising the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`.

You can connect to the Kafka topic using:

```shell
 docker-compose exec kafka kafka-console-consumer --topic customer_changes --from-beginning --bootstrap-server localhost:9092
```

Each interaction below will trigger a state change that will then be propagated to the Kafka topic.

* Create a customer with:

```shell
grpcurl --plaintext -d '{"customer_id": "wip", "email": "wip@example.com", "name": "Very Important", "address": {"street": "Road 1", "city": "The Capital"}}' localhost:9000  customer.api.CustomerService/Create
```

* Retrieve the customer:

```shell
grpcurl --plaintext -d '{"customer_id": "wip"}' localhost:9000  customer.api.CustomerService/GetCustomer
```

* Change name:

```shell
grpcurl --plaintext -d '{"customer_id": "wip", "new_name": "Most Important"}' localhost:9000 customer.api.CustomerService/ChangeName
```

* Change address:

```shell
grpcurl --plaintext -d '{"customer_id": "wip", "new_address": {"street": "Street 1", "city": "The City"}}' localhost:9000 customer.api.CustomerService/ChangeAddress
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

# Quickstart project: Customer Registry

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/services/development-process.html) in the documentation.


## Developing

This project demonstrates the use of Value Entity and View components.
To understand more about these components, see [Developing services](https://docs.kalix.io/services/)
and in particular the [Java section](https://docs.kalix.io/java/)


## Building

You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```shell
mvn compile
```


## Running Locally 

To run the example locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.

It also contains the configuration to start a local Kafka broker and a Zookeeper that the Kalix proxy will connect to.
To start the proxy, the Kafka broker, and the Zookeeper, run the following command from the root directory:

```shell
docker compose -f docker-compose.yml rm -f && docker compose -f docker-compose.yml up
```

It can make sense to delete the existing containers (if any exist) and start from scratch when starting up the Kafka container.

### Create topic for eventing out

Every time a customer receives a change, the application also persists that change to a topic in Kafka called `customer_changes`. You can see that this is set in customer_action.proto. In order for the changes to be written to this topic, you need to manually create the topic in Kafka. To do this, run the following command after the Docker container `java-protobuf-customer-registry-kafka-quickstart-kafka-1` is running.

    docker-compose exec broker kafka-topics --create --bootstrap-server \
    localhost:9092 --replication-factor 1 --partitions 1 --topic customer_changes

### Starting the application
To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:exec
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:


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

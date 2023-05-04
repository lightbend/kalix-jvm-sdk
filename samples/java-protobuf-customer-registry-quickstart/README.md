# Quickstart project: Customer Registry

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

To run the example locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:exec
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:


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
to create a project and then deploy your service into the project either by using `mvn deploy kalix:deploy` which
will conveniently package, publish your docker image, and deploy your service to Kalix, or by first packaging and
publishing the docker image through `mvn deploy` and then deploying the image
through the `kalix` CLI.

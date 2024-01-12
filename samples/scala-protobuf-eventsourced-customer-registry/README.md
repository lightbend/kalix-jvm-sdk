# Event Sourced Customer Registry Sample

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/developing/development-process-proto.html) in the documentation.

## Developing

This project demonstrates the use of Event Sourced Entity, View and Service to Service eventing components.
To understand more about these components, see [Developing services](https://docs.kalix.io/services/)
and in particular the [Scala section](https://docs.kalix.io/java/)

The project scala-customer-registry-subscriber is a downstream consumer of the Service to Service event stream
provided by this service.

## Building and running unit tests

To compile and test the code from the command line, use

```shell
sbt test
``

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
sbt runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`.

* Create a customer with:

```shell
grpcurl --plaintext -d '{"customer_id": "vip", "email": "vip@example.com", "name": "Very Important", "address": {"street": "Road 1", "city": "The Capital"}}' localhost:9000  customer.api.CustomerService/Create
```

* Retrieve the customer:

```shell
grpcurl --plaintext -d '{"customer_id": "vip"}' localhost:9000  customer.api.CustomerService/GetCustomer
```

* Query by name:

```shell
grpcurl --plaintext -d '{"customer_name": "Very Important"}' localhost:9000 customer.view.CustomerByName/GetCustomers
```

* Change name:

```shell
grpcurl --plaintext -d '{"customer_id": "vip", "new_name": "Most Important"}' localhost:9000 customer.api.CustomerService/ChangeName
```

* Change address:

```shell
grpcurl --plaintext -d '{"customer_id": "vip", "new_address": {"street": "Street 1", "city": "The City"}}' localhost:9000 customer.api.CustomerService/ChangeAddress
```

* Query the customer by name view:

```shell
grpcurl --plaintext -d '{"customer_name":"Bob"}' localhost:9000 customer.view.CustomerByName/GetCustomers
```

* Query the streaming customer by city (will stay running listing updates until cancelled with Ctrl+C):

```shell
grpcurl --plaintext -d '{"city":"Stockholm"}' localhost:9000 customer.view.CustomerByCityStreaming/GetCustomers
```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Install Kalix](https://docs.kalix.io/kalix/install-kalix.html)
and configure a Docker Registry to upload your docker image to.

You will need to set the `docker.username` system property when starting sbt to be able to publish the image, for example `sbt -Ddocker.username=myuser Docker/publish`.

If you are publishing to a different registry than docker hub, you will also need to specify what registry using the system property `docker.registry`.

Refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a Kalix project and then deploy your service into it through the `kalix` CLI.

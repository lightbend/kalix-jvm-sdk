# Shopping Cart example (using an Event Sourced Entity)

This example project implements an API for a shopping cart using a Kalix Event Sourced Entity

## See also

* process - for effective development it is useful to
  read [development process](https://docs.kalix.io/developing/development-process-proto.html)
* developing - it may be useful to read up
  on [developing with Java](https://docs.kalix.io/java/index.html)

## Building

To compile and test the code from the command line, use

```shell
sbt test
```

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
sbt runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`.
In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (
see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST
requests at the path `/[package].[entity name]/[method]`. For example, using `curl`.

* Send an AddItem command:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/cart/cart1/items/add -d '{"cart_id": "cart1", "product_id": "kalix-tshirt", "name": "Kalix t-shirt", "quantity": 3}' 
```

For example, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

* Send an AddItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "kalix-tshirt", "name": "Kalix t-shirt", "quantity": 3}' localhost:9000 com.example.shoppingcart.ShoppingCartService/AddItem
```

* Send a GetCart command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000 com.example.shoppingcart.ShoppingCartService/GetCart
```

* Send a RemoveItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "kalix-tshirt" }' localhost:9000 com.example.shoppingcart.ShoppingCartService/RemoveItem
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

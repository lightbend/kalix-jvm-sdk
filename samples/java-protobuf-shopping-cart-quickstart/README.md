# Quickstart project: Shopping Cart

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/developing/development-process-proto.html) in the documentation.

## Developing

This project demonstrates the use of an Event Sourced Entity.
To understand more about these components, see [Developing services](https://docs.kalix.io/services/)
and in particular the [Java Protobuf SDK section](https://docs.kalix.io/java-protobuf/)

## Building

You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```shell
mvn compile
```

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`.

For example, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

* Send an AddItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "kalix-tshirt", "name": "Kalix t-shirt", "quantity": 3}' localhost:9000  shopping.cart.api.ShoppingCart/AddItem
```

* Send a GetCart command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000  shopping.cart.api.ShoppingCart/GetCart
```

* Send a RemoveItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "kalix-tshirt" }' localhost:9000 shopping.cart.api.ShoppingCart/RemoveItem
```

In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. As defined in `shpping_cart_api.proto` (see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/cart/<cart_id>/items/add`.


* Send an AddItem command:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/cart/cart1/items/add -d '{"cart_id": "cart1", "product_id": "kalix-tshirt", "name": "Kalix t-shirt", "quantity": 3}'
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

# Shopping Cart example (using a Value Entity)

This example project implements an API for a shopping cart using a Kalix Value Entity

## See also

* process - for effective development it is useful to
  read [development process](https://docs.kalix.io/developing/development-process-proto.html)
* developing - it may be useful to read up
  on [developing with Java](https://docs.kalix.io/java/index.html)

## Building

You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```shell
mvn compile
```

## Running Locally

In order to run your application locally, you must run the Kalix proxy. The included `docker-compose` file
contains the configuration required to run the proxy for a locally running application. It also contains the
configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to. To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:exec
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`.
In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (
see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST
requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

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

## Running integration tests

The integration tests in `src/it` are added by setting `it` as test source directory.
To run the Integration Tests in `src/it/java` use

```shell
mvn verify -Pit
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

# Quickstart project: Shopping Cart

# TODO update for quickstart instructions

## Designing

To understand the Akka Serverless concepts that are the basis for this example, see [Designing services](https://developer.lightbend.com/docs/akka-serverless/services/development-process.html) in the documentation.


## Developing

This project demonstrates the use of an Event Sourced Entity.
To understand more about these components, see [Developing services](https://developer.lightbend.com/docs/akka-serverless/developing/index.html)
and in particular the [Java section](https://developer.lightbend.com/docs/akka-serverless/java-services/index.html)


## Building

You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```
mvn compile
```


## Running Locally

To run the example locally, you must run the Akka Serverless proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Akka Serverless proxy will connect to.
To start the proxy, run the following command from this directory:


```
docker-compose up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```
mvn compile exec:exec
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://developer.lightbend.com/docs/akka-serverless/java/proto.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:


* Send an AddItem command:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/cart/cart1/items/add -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}'
```

For example, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

* Send an AddItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' localhost:9000  shopping.cart.api.ShoppingCartApi/AddItem
```

* Send a GetCart command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000  shopping.cart.api.ShoppingCartApi/GetCart
```

* Send a RemoveItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt" }' localhost:9000 shopping.cart.api.ShoppingCartApi/RemoveItem
```

## Deploying

To deploy your service, install the `akkasls` CLI as documented in
[Setting up a local development environment](https://developer.lightbend.com/docs/akka-serverless/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://developer.lightbend.com/docs/akka-serverless/projects/container-registries.html)
for more information on how to make your docker image available to Akka Serverless.

Finally you can or use the [Akka Serverless Console](https://console.akkaserverless.com)
to create a project and then deploy your service into the project either by using `mvn deploy`,
through the `akkasls` CLI or via the web interface. When using `mvn deploy`, Maven will also
conveniently package and publish your docker image prior to deployment.

# Shopping Cart example (using a Value Entity)

This example project implements an API for a shopping cart using an Akka Serverless Value Entity

## See also

* designing - while designing your service it is useful to read [designing services](https://developer.lightbend.com/docs/akka-serverless/designing/index.html)
* developing - it may be useful to read up on [developing services](https://developer.lightbend.com/docs/akka-serverless/developing/index.html) and in particular the [Java section](https://developer.lightbend.com/docs/akka-serverless/java-services/index.html)

## Building

To build, at a minimum you need to generate and process sources, particularly when using an IDE.

```shell
mvn compile
```


## Running Locally

In order to run your application locally, you must run the Akka Serverless proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Akka Serverless proxy will connect to.
To start the proxy, run the following command from this directory:


```shell
docker-compose up
```

> On Linux this requires Docker 20.10 or later (https://github.com/moby/moby/pull/40007),
> or for a `USER_FUNCTION_HOST` environment variable to be set manually.

```shell
docker-compose -f docker-compose.yml -f docker-compose.linux.yml up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:java
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://developer.lightbend.com/docs/akka-serverless/java/proto.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

* Send an AddItem command:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.shoppingcart.ShoppingCartService/AddItem -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' 
```

For example, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

* Send an AddItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' localhost:9000  com.example.shoppingcart.ShoppingCartService/AddItem
```

* Send a GetCart command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000  com.example.shoppingcart.ShoppingCartService/GetCart
```

* Send a RemoveItem command:

```shell
grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt" }' localhost:9000 com.example.shoppingcart.ShoppingCartService/RemoveItem
```

## Running integration tests

The integration tests in `src/it` are added by setting it as test source directory.
To  Integration Tests in src/it/java with

```shell
mvn verify -Pit
```

## Deploying

To deploy your service, install the `akkasls` CLI as documented in
[Setting up a local development environment](https://developer.lightbend.com/docs/akka-serverless/getting-started/set-up-development-env.html)
and configure a Docker Registry to upload your docker image to.

You will need to update the `akkasls.dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://developer.lightbend.com/docs/akka-serverless/deploying/registries.html)
for more information on how to make your docker image available to Akka Serverless.

Finally, you can or use the [Akka Serverless Console](https://console.akkaserverless.com)
to create a project and then deploy your service into the project either by using `mvn deploy`,
through the `akkasls` CLI or via the web interface. When using `mvn deploy`, Maven will also
conveniently package and publish your docker image prior to deployment.

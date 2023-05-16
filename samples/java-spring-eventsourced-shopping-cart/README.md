# Event Sourced Shopping Cart

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/java/development-process.html) in the documentation.

## Developing

This project demonstrates the use of Value Entity and View components.
To understand more about these components, see [Developing services](https://docs.kalix.io/services/)
and in particular the [Java section](https://docs.kalix.io/java/)

## Building

Use Maven to build your project:

```shell
mvn compile
```

## Running Locally

When running a Kalix application locally, at least two applications are required. The current Kalix application and its companion Kalix Proxy.

To start the applications locally, call the following command:

```shell
mvn kalix:runAll
```

This command will start your Kalix application and a Kalix Proxy using the included [docker-compose.yml](./docker-compose.yml) file.

With both the proxy and your application running, once you have defined endpoints they should be available at `http://localhost:9000`.

## Exercising the service

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. For example, using `curl`:

- Add items to shopping cart

```shell
curl -i -XPOST -H "Content-Type: application/json" localhost:9000/cart/123/add -d '{"productId":"kalix-tshirt", "name":"Akka Tshirt", "quantity": 10}'
curl -i -XPOST -H "Content-Type: application/json" localhost:9000/cart/123/add -d '{"productId":"scala-tshirt", "name":"Scala Tshirt", "quantity": 20}'
```

- See current status of the shopping cart

```shell
curl -i -XGET -H "Content-Type: application/json" localhost:9000/cart/123
```

- Remove an item from the cart

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/cart/123/items/kalix-tshirt/remove
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

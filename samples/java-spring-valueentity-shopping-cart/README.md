# java-spring-valueentity-shoppingcart



To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/java/development-process.html) in the documentation.



This project contains the framework to create a Kalix application by adding Kalix components. To understand more about these components, see [Developing services](https://docs.kalix.io/services/). Spring-SDK is an experimental feature and so far there is no [official](https://docs.kalix.io/) documentation. Examples can be found [here](https://github.com/lightbend/kalix-jvm-sdk/tree/main/samples) in the folders with "spring" in their name.



Use Maven to build your project:

```shell
mvn compile
```



To run the example locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `spring-boot-maven-plugin` is used. Use the following command:

```shell
mvn spring-boot:run
```

## Exercising the service

With both the proxy and your application running, once you have defined endpoints they should be available at `http://localhost:9000`. 


* Adding a new item:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/cart/cart1/items/add -d '{"productId": "kalix-tshirt", "name": "Kalix t-shirt", "quantity": 3}' 
```

* Removing a new item:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/cart/cart1/items/add -d '{"productId": "kalix-tshirt", "name": "Kalix t-shirt", "quantity": 3}' 
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

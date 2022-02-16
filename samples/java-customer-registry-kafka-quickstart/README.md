# Quickstart project: Customer Registry

## Designing

To understand the Akka Serverless concepts that are the basis for this example, see [Designing services](https://developer.lightbend.com/docs/akka-serverless/services/development-process.html) in the documentation.


## Developing

This project demonstrates the use of Value Entity and View components.
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

It also contains the configuration to start a local Kafka broker and a Zookeeper that the Akka Serverless proxy will connect to.
To start the proxy, the Kafka broker, and the Zookeeper, run the following command from the kafka directory:

```
docker-compose up
```

### Starting the application
To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```
mvn compile exec:exec
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://developer.lightbend.com/docs/akka-serverless/java/proto.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:


* Create a customer with:
  ```
  grpcurl --plaintext -d '{"customer_id": "wip", "email": "wip@example.com", "name": "Very Important", "address": {"street": "Road 1", "city": "The Capital"}}' localhost:9000  customer.api.CustomerService/Create
  ```
* Retrieve the customer:
  ```
  grpcurl --plaintext -d '{"customer_id": "wip"}' localhost:9000  customer.api.CustomerService/GetCustomer
  ```
* Change name:
  ```
  grpcurl --plaintext -d '{"customer_id": "wip", "new_name": "Most Important"}' localhost:9000 customer.api.CustomerService/ChangeName
  ```
* Change address:
  ```
  grpcurl --plaintext -d '{"customer_id": "wip", "new_address": {"street": "Street 1", "city": "The City"}}' localhost:9000 customer.api.CustomerService/ChangeAddress
  ```

## Deploying

To deploy your service, install the `akkasls` CLI as documented in
[Setting up a local development environment](https://developer.lightbend.com/docs/akka-serverless/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://developer.lightbend.com/docs/akka-serverless/projects/container-registries.html)
for more information on how to make your docker image available to Akka Serverless.

Finally, you can use the [Akka Serverless Console](https://console.akkaserverless.com)
to create a project and then deploy your service into the project either by using `mvn deploy` which
will also conveniently package and publish your docker image prior to deployment, or by first packaging and
publishing the docker image through `mvn clean package docker:push -DskipTests` and then deploying the image
through the `akkasls` CLI or via the web interface.
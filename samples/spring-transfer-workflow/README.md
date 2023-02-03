# Workflow transfer sample

A simple workflow example of funds transfer between two wallets.

Used for code snippets in the Workflow documentation.


## Developing

This project demonstrates the use of Workflow Entity with Value Entities.

To understand more about these components, see [developing services](https://docs.kalix.io/services/)
and in particular the [developing with Spring section](https://docs.kalix.io/spring/).


## Building

Use Maven to build your project:

```
mvn compile
```


## Running Locally

To run the example locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```
docker-compose up
```

To start the application locally, use the following command:

```
mvn spring-boot:run
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. 


### Exercising the transfer

Create wallet `a` with an initial balance

```shell
curl -X POST http://localhost:9000/wallet/a/create/100
```

Create wallet `b` with an initial balance

```shell
curl -X POST http://localhost:9000/wallet/b/create/100
```

Get wallet `a` current balance

```shell
curl http://localhost:9000/wallet/a
```

Get wallet `b` current balance

```shell
curl http://localhost:9000/wallet/b
```

Start transfer from wallet `a` to wallet `b`

```shell
curl http://localhost:9000/transfer/1 \
  -X PUT \
  --header "Content-Type: application/json" \
  --data '{"from": "a", "to": "b", "amount": 10}'
```

Get transfer state

```shell
curl http://localhost:9000/transfer/1
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
to create a project and then deploy your service into the project either by using `mvn deploy` which
will also conveniently package and publish your docker image prior to deployment, or by first packaging and
publishing the docker image through `mvn clean package docker:push -DskipTests` and then deploying the image
through the `kalix` CLI.

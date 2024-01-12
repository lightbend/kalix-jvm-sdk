# Event Sourecd Counter

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

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

## Exercising the services

With both the Kalix Runtime and your service running, once you have defined endpoints they should be available at `http://localhost:9000`.

### Examples

- increase (or create) a counter named `hello` with value `10`

```shell
curl -XPOST localhost:9000/counter/hello/increase/10
```

- retrieve the value of a counter named `hello`

```shell
curl -XGET localhost:9000/counter/hello
```

- multiply existing counter named `hello` by value `5`

```shell
curl -XPOST localhost:9000/counter/hello/multiply/5
```

### Deploy

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

## Integration Tests

This sample showcases how to have integration tests with and without a real broker. Thus, to run the integration tests locally, you need to have Google PubSub Emulator running.

First run:
```shell
docker-compose up -d gcloud-pubsub-emulator
```

Then run:
```shell
mvn verify -Pit
```

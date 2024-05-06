# Tracing with OpenTelemetry in Java

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/java/development-process.html) in the documentation.

## Developing

This project demonstrates how to use tracing in a Kalix Service using Java SDK.
The service is based on a User entity that gets updated with a random name and random picture after created.
Such updates are done after calling an external API to get a random name and picture.
All the flow is traced (most of it automatically), and see the traces in Jaeger UI.


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

This command will start your Kalix service, a companion Kalix Runtime and Jaeger as configured in [docker-compose.yml](./docker-compose.yml) file.

## Exercising the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`.

- Add a new user

```shell
 curl -i -XPOST -H "Content-Type: application/json" localhost:9000/user/1/add -d '{"email":"john@doe.com"}'
```

- Now you can see the trace in Jaeger UI at http://localhost:16686
  - select "Kalix endpoint" and "Find all traces" to explore the trace

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

# Serving arbitrary web resources

Example of how to serve web resources like html files, js, css and images through Kalix via http.

Please look at the [documentation](https://docs.kalix.io/java-protobuf/serving-web-resources.html) for more details.

This project is based on the Kalix Maven archetype.

See the [Kickstart a Maven project](https://docs.kalix.io/java/kickstart.html) in the documentation for details.

## Building and running unit tests

To compile and test the code from the command line, use

```shell
mvn verify
```

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`.

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

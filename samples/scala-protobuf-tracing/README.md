# scala-protobuf-tracing

## Designing

While designing your service it is useful to read [designing services](https://docs.kalix.io/services/development-process.html).

## Developing

This project has a bare-bones skeleton service ready to go, but in order to adapt and
extend it it may be useful to read up on [developing services](https://docs.kalix.io/developing/index.html)
and in particular the [JVM section](https://docs.kalix.io/java-services/index.html).

## Building

You can use [sbt](https://www.scala-sbt.org/) to build your project,
which will also take care of generating code based on the `.proto` definitions:

```
sbt compile
```

## Running Locally

In order to run your application locally, you must run the Kalix proxy. The included `docker-compose.yml` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```
docker-compose up
```

> On Linux this requires Docker 20.10 or later (https://github.com/moby/moby/pull/40007),
> or for a `USER_FUNCTION_HOST` environment variable to be set manually.

To start the application locally, start it from your IDE or use:

```
sbt run
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

```shell
> curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.CounterService/GetCurrentCounter -d '{"counterId": "foo"}'
The command handler for `GetCurrentCounter` is not implemented, yet
```

For example, using [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```shell
> grpcurl -plaintext -d '{"counterId": "foo"}' localhost:9000 com.example.CounterService/GetCurrentCounter 
ERROR:
  Code: Unknown
  Message: The command handler for `GetCurrentCounter` is not implemented, yet
```

> Note: The failure is to be expected if you have not yet provided an implementation of `GetCurrentCounter` in
> your entity.

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your Docker image to.

You will need to set your `docker.username` as a system property:

```
sbt -Ddocker.username=mary docker:publish
```

Refer to [Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your Docker image available to Kalix.

You can now deploy your service through the [kalix](https://docs.kalix.io/kalix/using-cli.html) CLI:

```
$ kalix auth login
```

If this is your first time using Kalix, this will let you
register an account, create your first project and set it as the default.

Now:

```
$ kalix services deploy \
    my-service \
    my-container-uri/container-name:tag-name
```

Once the service has been successfully started (this may take a while),
you can create an ad-hoc proxy to call it from your local machine:

```
$ kalix services proxy my-service
Listening on 127.0.0.1:8080
```

Or expose it to the Internet:

```
kalix service expose my-service
```

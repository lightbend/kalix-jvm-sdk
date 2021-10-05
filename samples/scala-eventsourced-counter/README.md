# Implementing a Counter as a Event Sourced Entity


## Building and running unit tests

To compile and test the code from the command line, use

```shell
sbt test
```



## Running Locally

In order to run your application locally, you must run the Akka Serverless proxy. The included `docker compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Akka Serverless proxy will connect to.
To start the proxy, run the following command from this directory:

```
docker-compose up
```

> On Linux this requires Docker 20.10 or later (https://github.com/moby/moby/pull/40007),
> or for a `USER_FUNCTION_HOST` environment variable to be set manually.

To start the application locally, use the following command:

```
sbt run
```

For further details see [Running a service locally](https://developer.lightbend.com/docs/akka-serverless/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.lbcs.dev/js-services/proto.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.CounterService/GetCurrentCounter -d '{"counterId": "foo"}'
```

Or, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```shell
grpcurl -plaintext -d '{"counterId": "foo"}' localhost:9000 com.example.CounterService/GetCurrentCounter 
```

## Deploying

To deploy your service, install the `akkasls` CLI as documented in
[Setting up a local development environment](https://developer.lightbend.com/docs/akka-serverless/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to set the `docker.username` system property when starting sbt to be able to publish the image, for example `sbt -Ddocker.username=myuser docker:publish`. 

If you are publishing to a different registry than docker hub, you will also need to specify what registry using the system property `docker.registry`.

Refer to
[Configuring registries](https://developer.lightbend.com/docs/akka-serverless/projects/container-registries.html)
for more information on how to make your docker image available to Akka Serverless.

Finally, you can use the [Akka Serverless Console](https://console.akkaserverless.com)
to create an Akka Serverless project and then deploy your service into it 
through the `akkasls` CLI or via the web interface. 

# Implementing an Order Service as a Value Entity in combination with Timers

## Building and running unit tests

To compile and test the code from the command line, use

```shell
sbt test
```



## Running Locally

In order to run your application locally, you must run the Kalix proxy. The included `docker compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, use the following command:

```
sbt run
```

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

```shell
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.actions.Order/PlaceOrder -d '{ "item":"Pizza Margherita", "quantity":1 }'

# confirm or cancel before it expires
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.actions.Order/Confirm -d '{ "number" : "returned-order-number" }'
curl -XPOST -H "Content-Type: application/json" localhost:9000/com.example.actions.Order/Cancel -d '{ "number" : "returned-order-number" }'
```


Or, given [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```shell
grpcurl -plaintext -d '{ "item":"Pizza Margherita", "quantity":1 }' localhost:9000 com.example.actions.Order/PlaceOrder

# confirm or cancel before it expires
grpcurl -plaintext -d '{ "number" : "returned-order-number" }' localhost:9000 com.example.actions.Order/Confirm
grpcurl -plaintext -d '{ "number" : "returned-order-number" }' localhost:9000 com.example.actions.Order/Cancel
```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to set the `docker.username` system property when starting sbt to be able to publish the image, for example `sbt -Ddocker.username=myuser Docker/publish`. 

If you are publishing to a different registry than docker hub, you will also need to specify what registry using the system property `docker.registry`.

Refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a Kalix project and then deploy your service into it 
through the `kalix` CLI. 

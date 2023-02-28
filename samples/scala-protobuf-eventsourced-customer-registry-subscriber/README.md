# Customer Registry Subscriber Sample

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/services/development-process.html) in the documentation.


## Developing

This project demonstrates consumption of a Service to Service eventing publisher. It consumes events produced and published
by a separate service, implemented in the `scala-protobuf-eventsourced-customer-registry` sample.

## Running Locally

First start the `scala-protobuf-eventsourced-customer-registry` service and proxy. It will run with the default service and proxy ports (`8080` and `9000`).

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

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9001`.
Updates are logged as they arrived and can be queried by name through:


* Query the customer by name view:
  ```shell
  grpcurl --plaintext localhost:9000 customer.view.AllCustomersView/GetCustomers
  ```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

First deploy the `scala-protobuf-eventsourced-customer-registry` service, as that is the upstream event producer for service to service eventing
that we will consume.

Note that you will need to change the service to subscribe to according to what you deployed the event sourced customer 
registry service as in `view/customer_view.proto`: 

```protobuf
option (kalix.service).eventing.in.direct = {
    service: "my-customer-registry"
}
```

You will need to set the `docker.username` system property when starting sbt to be able to publish the image, for example `sbt -Ddocker.username=myuser Docker/publish`.

If you are publishing to a different registry than docker hub, you will also need to specify what registry using the system property `docker.registry`.

Refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a Kalix project and then deploy your service into it
through the `kalix` CLI. 

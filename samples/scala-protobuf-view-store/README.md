# Store example (using advanced Views)

A simple store example with products, customers, and orders.

Used for code snippets in the Views documentation.

## Building

You can use [sbt](https://www.scala-sbt.org/) to build this project,
which will also take care of generating code based on the `.proto` definitions:

```shell
sbt compile
```

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
sbt runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

## Exercise the service

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`.


Create some products:

```shell
grpcurl -d '{
    "productId": "P123",
    "productName": "Super Duper Thingamajig",
    "price": {"currency": "USD", "units": 123, "cents": 45}
  }' \
  --plaintext localhost:9000 \
  store.product.api.Products/Create
```

```shell
grpcurl -d '{
    "productId": "P987",
    "productName": "Awesome Whatchamacallit",
    "price": {"currency": "NZD", "units": 987, "cents": 65}
  }' \
  --plaintext localhost:9000 \
  store.product.api.Products/Create
```

Retrieve a product by id:

```shell
grpcurl -d '{"productId": "P123"}' \
  --plaintext localhost:9000 \
  store.product.api.Products/Get
```

Create a customer:

```shell
grpcurl -d '{
    "customerId": "C001",
    "email": "someone@example.com",
    "name": "Some Customer",
    "address": {"street": "123 Some Street", "city": "Some City"}
  }' \
  --plaintext localhost:9000 \
  store.customer.api.Customers/Create
```

Retrieve a customer by id:

```shell
grpcurl -d '{"customerId": "C001"}' \
  --plaintext localhost:9000 \
  store.customer.api.Customers/Get
```

Create customer orders for the products:

```shell
grpcurl -d '{
    "orderId": "O1234",
    "productId": "P123",
    "customerId": "C001",
    "quantity": 42
  }' \
  --plaintext localhost:9000 \
  store.order.api.Orders/Create
```

```shell
grpcurl -d '{
    "orderId": "O5678",
    "productId": "P987",
    "customerId": "C001",
    "quantity": 7
  }' \
  --plaintext localhost:9000 \
  store.order.api.Orders/Create
```

Retrieve an order by id:

```shell
grpcurl -d '{"orderId": "O5678"}' \
  --plaintext localhost:9000 \
  store.order.api.Orders/Get
```

Retrieve all product orders for a customer id using a view (with joins):

```shell
grpcurl -d '{"customerId": "C001"}' \
  --plaintext localhost:9000 \
  store.view.joined.JoinedCustomerOrders/Get
```

Retrieve all product orders for a customer id using a view (with joins and nested projection):

```shell
grpcurl -d '{"customerId": "C001"}' \
  --plaintext localhost:9000 \
  store.view.nested.NestedCustomerOrders/Get
```

Retrieve all product orders for a customer id using a view (with joins and structured projection):

```shell
grpcurl -d '{"customerId": "C001"}' \
  --plaintext localhost:9000 \
  store.view.structured.StructuredCustomerOrders/Get
```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Install Kalix](https://docs.kalix.io/kalix/install-kalix.html)
and configure a Docker Registry to upload your docker image to.

You will need to set your `docker.username` as a system property:

```shell
sbt -Ddocker.username=mary docker:publish
```

Refer to [Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your Docker image available to Kalix.

You can now deploy your service through the [kalix](https://docs.kalix.io/kalix/using-cli.html) CLI:

```shell
$ kalix auth login
```

If this is your first time using Kalix, this will let you
register an account, create your first project and set it as the default.

Now:

```shell
$ kalix services deploy \
    my-service \
    my-container-uri/container-name:tag-name
```

Once the service has been successfully started (this may take a while),
you can create an ad-hoc proxy to call it from your local machine:

```shell
$ kalix services proxy my-service
Listening on 127.0.0.1:8080
```

Or expose it to the Internet:

```shell
kalix service expose my-service
```

# Store example (using advanced Views)

A simple store example with products, customers, and orders.

Used for code snippets in the Views documentation.

## Building

You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```
mvn compile
```

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.


With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`.

### Exercising the services

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

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you use the `kalix` CLI to create a project as described in [Create a new Project](https://docs.kalix.io/projects/create-project.html). Once you have a project you can deploy your service into the project either 
by using `mvn deploy kalix:deploy` which will package, publish your docker image, and deploy your service to Kalix, 
or by first packaging and publishing the docker image through `mvn deploy` and 
then [deploying the image through the `kalix` CLI](https://docs.kalix.io/services/deploy-service.html#_deploy).

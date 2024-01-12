# Store example (using advanced Views)

A simple store example with products, customers, and orders.

Used for code snippets in the Views documentation.

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
mvn kalix:runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

## Exercising the services

With both the Kalix Runtime and your service running, once you have defined endpoints they should be available at `http://localhost:9000`.

Create some products:

```shell
curl localhost:9000/product/P123/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Super Duper Thingamajig",
    "price": {"currency": "USD", "units": 123, "cents": 45}
  }'
```

```shell
curl localhost:9000/product/P987/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Awesome Whatchamacallit",
    "price": {"currency": "NZD", "units": 987, "cents": 65}
  }'
```

Retrieve a product by id:

```shell
curl localhost:9000/product/P123
```

Create a customer:

```shell
curl localhost:9000/customer/C001/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "email": "someone@example.com",
    "name": "Some Customer",
    "address": {"street": "123 Some Street", "city": "Some City"}
  }'
 ```

Retrieve a customer by id:

```shell
curl localhost:9000/customer/C001
```

Create customer orders for the products:

```shell
curl localhost:9000/order/O1234/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "productId": "P123",
    "customerId": "C001",
    "quantity": 42
  }'
```

```shell
curl localhost:9000/order/O5678/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "productId": "P987",
    "customerId": "C001",
    "quantity": 7
  }'
```

Retrieve an order by id:

```shell
curl localhost:9000/order/O5678
```

Retrieve all product orders for a customer id using a view (with joins):

```shell
curl localhost:9000/joined-customer-orders/C001
```

Retrieve all product orders for a customer id using a view (with joins and nested projection):

```shell
curl localhost:9000/nested-customer-orders/C001
```

Retrieve all product orders for a customer id using a view (with joins and structured projection):

```shell
curl localhost:9000/structured-customer-orders/C001
```

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

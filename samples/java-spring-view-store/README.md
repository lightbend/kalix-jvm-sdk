# Store example (using advanced Views)

A simple store example with products, customers, and orders.

Used for code snippets in the Views documentation.


## Developing

This project demonstrates the use of advanced View components, with Event Sourced Entities and Value Entities.

To understand more about these components, see [developing services](https://docs.kalix.io/services/)
and in particular the [developing with Spring section](https://docs.kalix.io/spring/).


## Building

Use Maven to build your project:

```
mvn compile
```


## Running Locally

To run the example locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```
docker-compose up
```

To start the application locally, use the following command:

```
mvn spring-boot:run
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. 


### Exercising the services

Create some products:

```
curl localhost:9000/product/P123/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Super Duper Thingamajig",
    "price": {"currency": "USD", "units": 123, "cents": 45}
  }'
```

```
curl localhost:9000/product/P987/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Awesome Whatchamacallit",
    "price": {"currency": "NZD", "units": 987, "cents": 65}
  }'
```

Retrieve a product by id:

```
curl localhost:9000/product/P123
```

Create a customer:

```
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

```
curl localhost:9000/customer/C001
```

Create customer orders for the products:

```
curl localhost:9000/order/O1234/create \
  -XPOST \
  --header "Content-Type: application/json" \
  --data '{
    "productId": "P123",
    "customerId": "C001",
    "quantity": 42
  }'
```

```
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

```
curl localhost:9000/order/O5678
```

Retrieve all product orders for a customer id using a view (with joins):

```
curl localhost:9000/joined-customer-orders/C001
```

Retrieve all product orders for a customer id using a view (with joins and nested projection):

```
curl localhost:9000/nested-customer-orders/C001
```

Retrieve all product orders for a customer id using a view (with joins and structured projection):

```
curl localhost:9000/structured-customer-orders/C001
```


## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a project and then deploy your service into the project either by using `mvn deploy kalix:deploy` which
will conveniently package, publish your docker image, and deploy your service to Kalix, or by first packaging and
publishing the docker image through `mvn deploy` and then deploying the image
through the `kalix` CLI.

# Event Sourced Customer Registry Subscriber Sample

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/java/development-process.html) in the documentation.

The project `java-spring-eventsourced-customer-registry-subscriber` is a downstream consumer of the Service to Service event stream provided by `java-spring-eventsourced-customer-registry` project.

## Building

Use Maven to build your project:

```shell
mvn compile
```

## Running Locally

First start the `java-spring-eventsourced-customer-registry` service and proxy. It will run with the default service and proxy ports (`8080` and `9000`).

To start your service locally, run:

```shell
mvn kalix:runAll
```

### Create a customer

```shell
curl localhost:9001/customer/one/create \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{"email":"test@example.com","name":"Testsson","address":{"street":"Teststreet 25","city":"Testcity"}}'
```

This call is made on the subscriber service and will be forwarded to the `java-spring-eventsourced-customer-registry` service.

### Run a view query from this project

```shell
curl localhost:9001/customers/by_name/Testsson
```

The subscriber service will receive updates from customer-registry via service-to-service stream and update the view.

### Change name

```shell
curl localhost:9000/customer/one/changeName/Jan%20Banan -XPOST
```

This call is performed on the customer-registry directly.
  
### Check the view again

```shell
curl localhost:9001/customers/by_name/Jan%20Banan
```

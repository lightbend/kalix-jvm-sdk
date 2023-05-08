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

To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `spring-boot-maven-plugin` is used. Use the following command:

```shell
mvn spring-boot:run
```

Run commands against `java-spring-eventsourced-customer-registry` project.

* Create a customer with:

```shell
curl localhost:9000/customer/one/create \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{"email":"test@example.com","name":"Test Testsson","address":{"street":"Teststreet 25","city":"Testcity"}}'
```

* Change name:

```shell
curl localhost:9000/customer/one/changeName/Jan%20Banan -XPOST
```
  
Run a view query from this project.

```shell
curl localhost:9001/customers/by_name/Jan%20Banan
```

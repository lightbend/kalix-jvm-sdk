# Event Sourced Customer Registry Subscriber Sample

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/developing/development-process-proto.html) in the documentation.

The project `java-protobuf-eventsourced-customer-registry-subscriber` is a downstream consumer of the Service to Service event stream provided by `java-protobuf-eventsourced-customer-registry` project.

## Building

Use Maven to build your project:

```shell
mvn compile
```
## Running Locally

First start the `java-protobuf-eventsourced-customer-registry` service and proxy. It will run with the default service and proxy ports (`8080` and `9000`).

To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:exec
```
Run commands against `java-protobuf-eventsourced-customer-registry` project.

* Create a customer with:
  ```shell
  grpcurl --plaintext -d '{"customer_id": "wip", "email": "wip@example.com", "name": "Very Important", "address": {"street": "Road 1", "city": "The Capital"}}' localhost:9000  customer.api.CustomerService/Create
  ```
* Change name:
  ```shell
  grpcurl --plaintext -d '{"customer_id": "wip", "new_name": "Most Important"}' localhost:9000 customer.api.CustomerService/ChangeName
  ```
  
Run a view query from this project.

* Query all customers:
  ```shell
  grpcurl --plaintext localhost:9001 customer.view.AllCustomersView/GetCustomers
  ```

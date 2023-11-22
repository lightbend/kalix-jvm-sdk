# Event Sourced Customer Registry Sample

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/developing/development-process-proto.html) in the documentation.

This example includes the code snippets that are used in the Views documentation.

The project `java-protobuf-eventsourced-customer-registry-subscriber` is a downstream consumer of the Service to Service event stream
provided by this service.

## Building

Use Maven to build your project:

```shell
mvn compile
```

## Running Locally

To run the example locally, you must run the Kalix Runtime. The included `docker-compose` file contains the configuration required by the Kalix Runtime for a locally running service.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix Runtime will connect to.

To start your service locally, run:

```shell
mvn kalix:runAll
```

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`.

* Create a customer with:

```shell
grpcurl --plaintext -d '{"customer_id": "vip", "email": "vip@example.com", "name": "Very Important", "address": {"street": "Road 1", "city": "The Capital"}}' localhost:9000  customer.api.CustomerService/Create
```

* Retrieve the customer:

```shell
grpcurl --plaintext -d '{"customer_id": "vip"}' localhost:9000  customer.api.CustomerService/GetCustomer
```
* Query by name:

```shell
grpcurl --plaintext -d '{"customer_name": "Very Important"}' localhost:9000 customer.view.CustomerByName/GetCustomers
```

* Change name:

```shell
grpcurl --plaintext -d '{"customer_id": "vip", "new_name": "Most Important"}' localhost:9000 customer.api.CustomerService/ChangeName
```
  
* Change address:

```shell
grpcurl --plaintext -d '{"customer_id": "vip", "new_address": {"street": "Street 1", "city": "The City"}}' localhost:9000 customer.api.CustomerService/ChangeAddress
```

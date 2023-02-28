# Views documentation example

This example includes the code snippets that are used in the Views documentation.

To run the example locally:

* Start the example:
  * publish relevant projects for use from Maven
    ```
    ./publishLocalM2.sh
    ```
  * trigger codegen, compile and run from Maven
    ```
    cd samples/java-protobuf-eventsourced-customer-registry
    mvn -Dkalix-sdk.version=0.7.0-beta....-dev-SNAPSHOT compile exec:exec
    ```

* Start the proxy
  * with in-memory store: `sbt proxy-core/run`
  * or with local Spanner emulator:
    * start the Spanner emulator: `docker run -p 9010:9010 -p 9020:9020 gcr.io/cloud-spanner-emulator/emulator`
    * `sbt proxy-spanner/run`
* Create a customer with:
  ```shell
  grpcurl --plaintext -d '{"customer_id": "wip", "email": "wip@example.com", "name": "Very Important", "address": {"street": "Road 1", "city": "The Capital"}}' localhost:9000  customer.api.CustomerService/Create
  ```
* Retrieve the customer:
  ```shell
  grpcurl --plaintext -d '{"customer_id": "wip"}' localhost:9000  customer.api.CustomerService/GetCustomer
  ```
* Query by name:
  ```shell
  grpcurl --plaintext -d '{"customer_name": "Very Important"}' localhost:9000 customer.view.CustomerByName/GetCustomers
  ```
* Change name:
  ```shell
  grpcurl --plaintext -d '{"customer_id": "wip", "new_name": "Most Important"}' localhost:9000 customer.api.CustomerService/ChangeName
  ```
* Change address:
  ```shell
  grpcurl --plaintext -d '{"customer_id": "wip", "new_address": {"street": "Street 1", "city": "The City"}}' localhost:9000 customer.api.CustomerService/ChangeAddress
  ```

# Shopping Cart example (using a Value Entity)

## Running locally

* Start the example:
  * from sbt:
    ```
    sbt java-valueentity-shopping-cart/run
    ```
  * or Maven
    ```
    cd samples/java-valueentity-shopping-cart
    mvn compile exec:java
    ```
* Start the proxy
  * with in-memory store:
    ```
    sbt proxy-core/run
    ```
  * or with local Spanner emulator:
    * start the Spanner emulator:
      ```
      docker run -p 9010:9010 -p 9020:9020 gcr.io/cloud-spanner-emulator/emulator`
      sbt proxy-spanner/run
      ```
* Send an AddItem command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' localhost:9000  com.example.valueentity.shoppingcart.ShoppingCartService/AddItem
  ```

* Send a GetCart command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000  com.example.valueentity.shoppingcart.ShoppingCartService/GetCart
  ```

* Send a RemoveItem command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "quantity": -1}' localhost:9000 com.example.valueentity.shoppingcart.ShoppingCartService/RemoveItem

## Running integration tests

* from sbt:
  ```
  sbt java-valueentity-shopping-cart/It/test
  ```

* from Maven:
  (The integration tests in `src/it` are added by setting it as test source directory.)
  ```
  mvn test
  ```

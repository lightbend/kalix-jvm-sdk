# Eventing example

This example show cases the following eventing features:

* Publishing of events from the event log to Google Pub Sub, see PublishAdded/PublishRemoved in [topic-publisher.proto](../../protocols/example/eventing/shoppingcart/topic-publisher.proto)
* Reading of events from the event log and forwarding to a ValueEntity, see ForwardAdded/ForwardRemoved in [to-product-popularity.proto](../../protocols/example/eventing/shoppingcart/to-product-popularity.proto)
* Reading of events from Google Pub Suc topic, see ProcessAdded/ProcessRemoved in [shopping-cart-analytics.proto](../../protocols/example/eventing/shoppingcart/shopping-cart-analytics.proto)

To run the example locally with the GooglePubSub emulator:

* Start the emulator: `gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085`
* Start the example: `sbt java-eventing-shopping-cart/run`
* Start the proxy: `sbt -Dakkaserverless.proxy.eventing.support=google-pubsub proxy-core/run`
  * note that this overrides the akkaserverless.proxy.eventing.support defined in `dev-mode.conf`
* Send an AddItem command: 
  `grpcurl --plaintext -d '{"user_id": "foo", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' localhost:9000  shopping.cart.api.ShoppingCartService/AddItem`
    * This will be published to the `shopping-cart-events` topic (via `TopicPublisherAction`) and received by the `ShoppingCartAnalyticsAction`.
    * This will be converted to commands and sent to `ProductPopularityEntity` via `ToProductPopularityAction`
* Send a RemoveItem command: `grpcurl --plaintext -d '{"user_id": "foo", "product_id": "akka-tshirt", "quantity": -1}' localhost:9000 shopping.cart.api.ShoppingCartService/RemoveItem`
* Check product popularity with `grpcurl --plaintext -d '{"productId": "akka-tshirt"}' localhost:9000  shopping.product.api.ProductPopularityService/GetPopularity` 
* Simulate a JSON message from an external system via Google Cloud Pub/Sub:
  (JSON sent to the Pub/Sub HTTP API in base64 encoding, it gets parsed into `com.akkaserverless.samples.eventing.shoppingcart.TopicMessage`)
  ```json
  {
    "operation": "add",
    "userId": "user-7539",
    "productId": "akkaserverless-tshirt",
    "name": "Akka Serverless T-Shirt",
    "quantity": 5
  }
  ```
  
  Sending the above message to the topic:
  ```
  curl -X POST \
    http://localhost:8085/v1/projects/test/topics/shopping-cart-json:publish \
    -H 'Content-Type: application/json' \
    -d '{
    "messages": [
      {
        "attributes": {
          "Content-Type": "application/json"
        },
        "data": "ewogICJvcGVyYXRpb24iOiAiYWRkIiwKICAidXNlcklkIjogInVzZXItNzUzOSIsCiAgInByb2R1Y3RJZCI6ICJha2thc2VydmVybGVzcy10c2hpcnQiLAogICJuYW1lIjogIkFra2EgU2VydmVybGVzcyBULVNoaXJ0IiwKICAicXVhbnRpdHkiOiA1Cn0K"
      }
    ]
  }'
  ```
* Send a message with CloudEvent metadata containing the protobuf serialized message `shopping.cart.api.TopicOperation`
  the message was encoded with
  ```java
  Base64.getEncoder()
      .encodeToString(
          ShoppingCartTopic.TopicOperation.newBuilder()
              .setOperation("add")
              .setUserId("user-0156")
              .setProductId("akkaserverless-socks")
              .setName("Akka Serverless pair of socks")
              .setQuantity(2)
              .build()
              .toByteArray())
  ```

  Sending the above message to the topic:
  ```
  curl -X POST \
    http://localhost:8085/v1/projects/test/topics/shopping-cart-protobuf-cloudevents:publish \
    -H 'Content-Type: application/json' \
    -d '{
    "messages": [
      {
        "attributes": {
          "ce-specversion": "1.0",
          "Content-Type": "application/protobuf",
          "ce-type": "shopping.cart.api.TopicOperation"  
        },
        "data": "CgNhZGQSCXVzZXItMDE1NhoUYWtrYXNlcnZlcmxlc3Mtc29ja3MiHUFra2EgU2VydmVybGVzcyBwYWlyIG9mIHNvY2tzKAI="
      }
    ]
  }'
  ```

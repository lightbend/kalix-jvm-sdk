# Eventing example

This example showcases the following eventing features:

* Publishing of events from an Event Sourced Entity to Google Pub Sub, see PublishAdded/PublishRemoved in [topic-publisher.proto](../../protocols/example/eventing/shoppingcart/topic-publisher.proto)
* Reading of events from an Event Sourced Entity and forwarding to a ValueEntity, see ForwardAdded/ForwardRemoved in [to-product-popularity.proto](../../protocols/example/eventing/shoppingcart/to-product-popularity.proto)
* Reading of events from Google Pub Sub topic, see ProcessAdded/ProcessRemoved in [shopping-cart-analytics.proto](../../protocols/example/eventing/shoppingcart/shopping-cart-analytics.proto)

To run the example locally with the GooglePubSub emulator: (See below for instructions to run against real Pub/Sub)

* Start the pubsub emulator: `gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085`
* Start the example:
  * from sbt: `sbt java-eventing-shopping-cart/run`
  * or mvn
    ```
    sbt sdkJava/publishM2 testkit/publishM2
    export AKKASERVERLESS_JAVA_SDK_VERSION="0.7.0-beta....-SNAPSHOT"
    cd samples/java-eventing-shopping-cart
    mvn compile exec:java
    ```
> On Linux, the application will bind to localhost which will not be accessible from the docker containers depending on the
> docker networking model. Use `HOST=0.0.0.0 mvn compile exec:java`, instead to make the user application bind to all interfaces.

* Start the proxy
  * with in-memory store: `sbt -Dakkaserverless.proxy.eventing.support=google-pubsub proxy-core/run`
    * note that this overrides the akkaserverless.proxy.eventing.support defined in `dev-mode.conf`
  * or with local Spanner emulator:
    * start the Spanner emulator: `docker run -p 9010:9010 -p 9020:9020 gcr.io/cloud-spanner-emulator/emulator`
    * `sbt -Dakkaserverless.proxy.eventing.support=google-pubsub proxy-spanner/run`
      * note that this overrides the akkaserverless.proxy.eventing.support defined in `spanner-dev-mode.conf`
* Send an AddItem command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' localhost:9000  shopping.cart.api.ShoppingCartService/AddItem
  ```
    * This will be published to the `shopping-cart-events` topic (via `TopicPublisherAction`) and received by the `ShoppingCartAnalyticsAction`.
    * This will be converted to commands and sent to `ProductPopularityEntity` via `ToProductPopularityAction`
* Send a GetCart command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000  shopping.cart.api.ShoppingCartService/GetCart
  ```
* Send a RemoveItem command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1", "product_id": "akka-tshirt", "quantity": -1}' localhost:9000 shopping.cart.api.ShoppingCartService/RemoveItem
* Check product popularity with:
  ```
  grpcurl --plaintext -d '{"productId": "akka-tshirt"}' localhost:9000  shopping.product.api.ProductPopularityService/GetPopularity
  ```
* Send a CheckoutCart command:
  ```
  grpcurl --plaintext -d '{"cart_id": "cart1"}' localhost:9000  shopping.cart.api.ShoppingCartService/CheckoutCart
  ```
* Find carts checked out after a given time:
  ```
  grpcurl --plaintext -d '{"timestamp": "1619446995774"}' localhost:9000  shopping.cart.view.ShoppingCartViewService/GetCheckedOutCarts
  ```
* Simulate a JSON message from an external system via Google Cloud Pub/Sub:
  (JSON sent to the Pub/Sub HTTP API in base64 encoding, it gets parsed into `com.akkaserverless.samples.eventing.shoppingcart.TopicMessage`)
  ```json
  {
    "operation": "add",
    "cartId": "cart-7539",
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
              .setCartId("cart-0156")
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

## Setup with real Google Pub/Sub

Choose the Google Cloud project to use, this uses `akka-serverless-playground`

```shell
GCP_PROJECT_ID=akka-serverless-playground
gcloud auth login
gcloud projects list
gcloud config set project ${GCP_PROJECT_ID}
# create key
gcloud iam service-accounts create akka-serverless-broker
gcloud projects add-iam-policy-binding ${GCP_PROJECT_ID} \
    --member "serviceAccount:akka-serverless-broker@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
    --role "roles/pubsub.editor"
gcloud iam service-accounts keys create keyfile.json \
    --iam-account akka-serverless-broker@${GCP_PROJECT_ID}.iam.gserviceaccount.com
```

Comment out the whole `akkaserverless.proxy.eventing` section in `proxy/core/src/main/resources/dev-mode.conf` to fully rely on `reference.conf`.

```shell
export EVENTING_SUPPORT="google-pubsub"
export PUBSUB_PROJECT_ID=${GCP_PROJECT_ID}
export PUBSUB_APPLICATION_CREDENTIALS=${PWD}/keyfile.json
```

Create the topics (subscriptions are auto-created)

```shell
gcloud beta pubsub topics create shopping-cart-events
gcloud beta pubsub topics create shopping-cart-protobuf-cloudevents
gcloud beta pubsub topics create shopping-cart-json
```

## Running integration tests locally

Start the pubsub emulator:
```
gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085
```

Publish the current proxy locally for use via the testkit and TestContainers
```
sbt proxy-core/Docker/publishLocal
```

Run the integration tests
```
sbt java-eventing-shopping-cart/It/test
```

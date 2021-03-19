# Eventing example

This example show cases the following eventing features:

* Publishing of events from the event log to Google Pub Sub, see PublishAdded/PublishRemoved in [topic-publisher.proto](../../protocols/example/eventing/shoppingcart/topic-publisher.proto)
* Reading of events from the event log and forwarding to a ValueEntity, see ForwardAdded/ForwardRemoved in [to-product-popularity.proto](../../protocols/example/eventing/shoppingcart/to-product-popularity.proto)
* Reading of events from Google Pub Suc topic, see ProcessAdded/ProcessRemoved in [shopping-cart-analytics.proto](../../protocols/example/eventing/shoppingcart/shopping-cart-analytics.proto)

To run the example locally with the GooglePubSub emulator:

* Start the emulator: `gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085`
* Set `akkaserverless.proxy.eventing.support` to `google-pubsub` e.g. by changing in `dev-mode.conf`
* Start the example: `sbt java-eventing-shopping-cart/run`
* Start the proxy: `sbt proxy-core/run`
* Send an AddItem command: 
  `grpcurl --plaintext -d '{"user_id": "foo", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 3}' localhost:9000  shopping.cart.api.ShoppingCartService/AddItem`
    * This will be published to the `shopping-cart-events` topic (via `TopicPublisherAction`) and received by the `ShoppingCartAnalyticsAction`.
    * This will be converted to commands and sent to `ProductPopularityEntity` via `ToProductPopularityAction`
* Send a RemoveItem command: `grpcurl --plaintext -d '{"user_id": "foo", "product_id": "akka-tshirt", "name": "Akka t-shirt", "quantity": 1}' localhost:9000 shopping.cart.api.ShoppingCartService/RemoveItem`
* Check product popularity with `grpcurl -d '{"productId": "akka-tshirt"}' -plaintext localhost:9000  shopping.product.api.ProductPopularity/GetPopularity` 


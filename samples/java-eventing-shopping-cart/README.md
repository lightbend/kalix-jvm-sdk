# Eventing example

This example show cases the following eventing features:

* Publishing a subset of events to Google Pub Sub, see PublishAdded/ProcessIgnore in [shoppingcart-publisher.proto](../../protocols/example/eventing/shoppingcart/shoppingcart-publisher.proto)
* Reading a subset of events via an event log, see ProcessRemoved/ProcessIgnore in [shoppingcart-publisher.proto](../../protocols/example/eventing/shoppingcart/shoppingcart-publisher.proto)
* Reading a subset of events via a topic, see ProcessAddedViaTopic in [shoppingcart-publisher.proto](../../protocols/example/eventing/shoppingcart/shoppingcart-publisher.proto)

To run the example locally with the GooglePubSub emulator:

* Start the emulator: `gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085`
* Set `akkaserverless.proxy.eventing.support` to `google-pubsub` e.g. by changing in `dev-mode.conf`
* Start the example: `sbt java-eventing-shopping-cart/run`
* Start the proxy: `sbt proxy-core/run`
* Send an AddItem command: `grpcurl --plaintext -d '{"user_id": "foo", "product_id": "a", "name": "A", "quantity": 3}' localhost:9000 com.example.eventing.shoppingcart.ShoppingCart/AddItem`
    * This will be published to the `allevents` topic and received by the analytics action.
* Send a RemoveItem command: `grpcurl --plaintext -d '{"user_id": "foo", "product_id": "a"}' localhost:9000 com.example.eventing.shoppingcart.ShoppingCart/RemoveItem`
    * This will be received by the analytics action via the event log

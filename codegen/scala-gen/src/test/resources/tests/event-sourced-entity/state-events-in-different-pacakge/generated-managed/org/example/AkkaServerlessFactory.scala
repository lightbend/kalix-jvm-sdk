package org.example

import kalix.scalasdk.AkkaServerless
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.eventsourcedentity.domain.Counter
import org.example.eventsourcedentity.domain.CounterProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createCounter: EventSourcedEntityContext => Counter): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(CounterProvider(createCounter))
  }
}

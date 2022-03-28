package org.example

import kalix.scalasdk.AkkaServerless
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.eventsourcedentity.CounterServiceEntity
import org.example.eventsourcedentity.CounterServiceEntityProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createCounterServiceEntity: EventSourcedEntityContext => CounterServiceEntity): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(CounterServiceEntityProvider(createCounterServiceEntity))
  }
}

package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.eventsourcedentity.counter_api.CounterServiceEntity
import org.example.eventsourcedentity.counter_api.CounterServiceEntityProvider

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

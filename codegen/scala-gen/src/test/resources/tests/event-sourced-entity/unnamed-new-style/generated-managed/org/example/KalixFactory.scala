package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.eventsourcedentity.CounterServiceEntity
import org.example.eventsourcedentity.CounterServiceEntityProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createCounterServiceEntity: EventSourcedEntityContext => CounterServiceEntity): Kalix = {
    val kalix = Kalix()
    kalix
      .register(CounterServiceEntityProvider(createCounterServiceEntity))
  }
}

package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.eventsourcedentity.domain.Counter
import org.example.eventsourcedentity.domain.CounterProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createCounter: EventSourcedEntityContext => Counter): Kalix = {
    val kalix = Kalix()
    kalix
      .register(CounterProvider(createCounter))
  }
}

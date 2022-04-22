package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.valueentity.ValueEntityContext
import org.example.valueentity.CounterServiceEntity
import org.example.valueentity.CounterServiceEntityProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createCounterServiceEntity: ValueEntityContext => CounterServiceEntity): Kalix = {
    val kalix = Kalix()
    kalix
      .register(CounterServiceEntityProvider(createCounterServiceEntity))
  }
}

package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.valueentity.ValueEntityContext
import org.example.valueentity.domain.Counter
import org.example.valueentity.domain.CounterProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createCounter: ValueEntityContext => Counter): Kalix = {
    val kalix = Kalix()
    kalix
      .register(CounterProvider(createCounter))
  }
}

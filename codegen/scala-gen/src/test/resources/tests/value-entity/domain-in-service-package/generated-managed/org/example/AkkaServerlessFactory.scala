package org.example

import kalix.scalasdk.AkkaServerless
import kalix.scalasdk.valueentity.ValueEntityContext
import org.example.valueentity.Counter
import org.example.valueentity.CounterProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createCounter: ValueEntityContext => Counter): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(CounterProvider(createCounter))
  }
}

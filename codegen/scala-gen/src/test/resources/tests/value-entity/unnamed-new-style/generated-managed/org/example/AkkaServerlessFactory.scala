package org.example

import kalix.scalasdk.AkkaServerless
import kalix.scalasdk.valueentity.ValueEntityContext
import org.example.valueentity.CounterServiceEntity
import org.example.valueentity.CounterServiceEntityProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createCounterServiceEntity: ValueEntityContext => CounterServiceEntity): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(CounterServiceEntityProvider(createCounterServiceEntity))
  }
}

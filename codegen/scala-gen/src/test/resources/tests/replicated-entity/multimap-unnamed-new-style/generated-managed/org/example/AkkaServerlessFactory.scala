package org.example

import com.example.replicated.multimap.MultiMapServiceEntity
import com.example.replicated.multimap.MultiMapServiceEntityProvider
import kalix.scalasdk.AkkaServerless
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createMultiMapServiceEntity: ReplicatedEntityContext => MultiMapServiceEntity): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(MultiMapServiceEntityProvider(createMultiMapServiceEntity))
  }
}

package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.example.replicated.multimap.multi_map_api.MultiMapServiceEntity
import com.example.replicated.multimap.multi_map_api.MultiMapServiceEntityProvider

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

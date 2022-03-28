package org.example

import com.example.replicated.multimap.MultiMapServiceEntity
import com.example.replicated.multimap.MultiMapServiceEntityProvider
import kalix.scalasdk.Kalix
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createMultiMapServiceEntity: ReplicatedEntityContext => MultiMapServiceEntity): Kalix = {
    val kalix = Kalix()
    kalix
      .register(MultiMapServiceEntityProvider(createMultiMapServiceEntity))
  }
}

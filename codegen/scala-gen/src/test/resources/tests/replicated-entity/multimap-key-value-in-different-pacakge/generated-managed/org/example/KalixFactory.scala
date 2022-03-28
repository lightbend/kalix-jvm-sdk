package org.example

import com.example.replicated.multimap.domain.SomeMultiMap
import com.example.replicated.multimap.domain.SomeMultiMapProvider
import kalix.scalasdk.Kalix
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createSomeMultiMap: ReplicatedEntityContext => SomeMultiMap): Kalix = {
    val kalix = Kalix()
    kalix
      .register(SomeMultiMapProvider(createSomeMultiMap))
  }
}

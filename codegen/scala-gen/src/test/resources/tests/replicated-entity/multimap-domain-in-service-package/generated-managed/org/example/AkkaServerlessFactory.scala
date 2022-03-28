package org.example

import com.example.replicated.multimap.SomeMultiMap
import com.example.replicated.multimap.SomeMultiMapProvider
import kalix.scalasdk.AkkaServerless
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createSomeMultiMap: ReplicatedEntityContext => SomeMultiMap): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(SomeMultiMapProvider(createSomeMultiMap))
  }
}

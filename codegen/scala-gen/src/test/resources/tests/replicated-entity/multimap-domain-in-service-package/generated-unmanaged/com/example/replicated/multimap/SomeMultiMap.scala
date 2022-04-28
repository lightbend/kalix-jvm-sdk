package com.example.replicated.multimap

import com.example.replicated.multimap
import com.google.protobuf.empty.Empty
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedMultiMap

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeMultiMap(context: ReplicatedEntityContext) extends AbstractSomeMultiMap {


  def put(currentData: ReplicatedMultiMap[SomeKey, SomeValue], putValue: PutValue): ReplicatedEntity.Effect[Empty] =
    effects.error("The command handler for `Put` is not implemented, yet")

}

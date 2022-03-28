package com.example.replicated.multimap

import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedMultiMap
import kalix.scalasdk.replicatedentity.ReplicatedMultiMapEntity
import com.example.replicated.multimap
import com.example.replicated.multimap.domain.SomeKey
import com.example.replicated.multimap.domain.SomeValue
import com.google.protobuf.empty.Empty

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractMultiMapServiceEntity extends ReplicatedMultiMapEntity[SomeKey, SomeValue] {

  def put(currentData: ReplicatedMultiMap[SomeKey, SomeValue], putValue: PutValue): ReplicatedEntity.Effect[Empty]

}

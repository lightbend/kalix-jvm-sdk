package com.example.replicated.multimap.domain

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMapEntity
import com.example.replicated.multimap.multi_map_api
import com.google.protobuf.empty.Empty

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** A replicated entity. */
abstract class AbstractSomeMultiMap extends ReplicatedMultiMapEntity[String, Double] {

  /** Command handler for "Put". */
  def put(currentData: ReplicatedMultiMap[String, Double], putValue: multi_map_api.PutValue): ReplicatedEntity.Effect[Empty]

}

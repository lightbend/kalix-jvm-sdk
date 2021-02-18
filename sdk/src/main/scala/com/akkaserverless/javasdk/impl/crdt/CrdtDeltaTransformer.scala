/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.crdt.CrdtDelta

private[crdt] object CrdtDeltaTransformer {

  def create(delta: CrdtDelta, anySupport: AnySupport): InternalCrdt = {
    val crdt = delta.delta match {
      case CrdtDelta.Delta.Gcounter(_) =>
        new GCounterImpl
      case CrdtDelta.Delta.Pncounter(_) =>
        new PNCounterImpl
      case CrdtDelta.Delta.Gset(_) =>
        new GSetImpl[Any](anySupport)
      case CrdtDelta.Delta.Orset(_) =>
        new ORSetImpl[Any](anySupport)
      case CrdtDelta.Delta.Flag(_) =>
        new FlagImpl
      case CrdtDelta.Delta.Lwwregister(_) =>
        new LWWRegisterImpl[Any](anySupport)
      case CrdtDelta.Delta.Ormap(_) =>
        new ORMapImpl[Any, InternalCrdt](anySupport)
      case CrdtDelta.Delta.Vote(_) =>
        new VoteImpl
    }
    crdt.applyDelta(delta.delta)
    crdt
  }

}

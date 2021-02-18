/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt.Crdt
import com.akkaserverless.protocol.crdt.CrdtDelta

private[crdt] trait InternalCrdt extends Crdt {
  def name: String
  def hasDelta: Boolean
  def delta: CrdtDelta.Delta
  def resetDelta(): Unit
  def applyDelta: PartialFunction[CrdtDelta.Delta, Unit]
}

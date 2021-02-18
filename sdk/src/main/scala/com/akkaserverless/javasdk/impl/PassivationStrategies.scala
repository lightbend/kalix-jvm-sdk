/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk.PassivationStrategy

import java.time.Duration

private[impl] case class Timeout private (duration: Option[Duration]) extends PassivationStrategy {

  def this() {
    this(None) // use the timeout from the default or customized settings
  }

  def this(duration: Duration) {
    this(Some(duration))
  }
}

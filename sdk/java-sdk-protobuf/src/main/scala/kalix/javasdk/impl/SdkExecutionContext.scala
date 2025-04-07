/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.stream.ActorAttributes
import akka.stream.Attributes

import scala.concurrent.ExecutionContext

/**
 * INTERNAL API
 */
@InternalApi
object SdkExecutionContext {
  val DispatcherName: String = "kalix.sdk-dispatcher"
  def apply(system: ActorSystem): ExecutionContext = system.dispatchers.lookup(DispatcherName)

  val streamDispatcher: Attributes = ActorAttributes.dispatcher(DispatcherName)

}

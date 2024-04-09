/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.SystemMaterializer
import kalix.javasdk.Context

/**
 * INTERNAL API
 */
private[impl] trait ActivatableContext extends Context {
  private final var active = true
  final def deactivate(): Unit = active = false
  final def checkActive(): Unit = if (!active) throw new IllegalStateException("Context no longer active!")
}

/**
 * INTERNAL API
 */
private[kalix] trait InternalContext {
  def getComponentGrpcClient[T](serviceClass: Class[T]): T

  /**
   * Intended to be used by component calls, initially to give to the called component access to the trace parent from
   * the caller. It's empty by default because only actions and workflows can to call other components. Of the two, only
   * actions have traces and can pass them around using `protected final Component components()`.
   */
  def componentCallMetadata: MetadataImpl = MetadataImpl.Empty
}

/**
 * INTERNAL API
 */
abstract class AbstractContext(system: ActorSystem) extends Context with InternalContext {

  override def materializer(): Materializer =
    SystemMaterializer(system).materializer

  def getComponentGrpcClient[T](serviceClass: Class[T]): T =
    GrpcClients(system).getComponentGrpcClient(serviceClass)

}

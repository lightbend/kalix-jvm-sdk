/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import akka.stream.Materializer
import kalix.scalasdk.Context
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.testkit.MockRegistry

class AbstractTestKitContext(mockRegistry: MockRegistry = MockRegistry.empty) extends Context with InternalContext {
  override def materializer(): Materializer =
    throw new UnsupportedOperationException("Accessing the materializer from testkit not supported yet")
  def getComponentGrpcClient[T](serviceClass: Class[T]): T = {
    mockRegistry
      .asInstanceOf[MockRegistryImpl]
      .get(serviceClass)
      .getOrElse(throw new NoSuchElementException(
        s"Could not find mock for component of type $serviceClass. Hint: use ${classOf[MockRegistry].getName} to provide an instance when testing services calling other components."))
  }
}

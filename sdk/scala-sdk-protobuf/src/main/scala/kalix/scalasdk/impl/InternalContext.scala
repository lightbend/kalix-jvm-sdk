/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl

import kalix.javasdk

/**
 * INTERNAL API
 */
// note cannot be package private since used by generated code
trait InternalContext {
  def getComponentGrpcClient[T](serviceClass: Class[T]): T

  /**
   * Intended to be used by component calls, initially to give to the called component access to the trace parent from
   * the caller. It's empty by default because only actions and workflows can to call other components. Of the two, only
   * actions have traces and can pass them around using `def components`.
   */
  def componentCallMetadata: MetadataImpl = MetadataImpl(javasdk.impl.MetadataImpl.Empty)
}

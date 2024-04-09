/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit

import kalix.scalasdk.DeferredCall

trait DeferredCallDetails[I, O] extends DeferredCall[I, O] {

  /** @return The name of the service being called */
  def serviceName: String

  /** @return The method name being called */
  def methodName: String
}

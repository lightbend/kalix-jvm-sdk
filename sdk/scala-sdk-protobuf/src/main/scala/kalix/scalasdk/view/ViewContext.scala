/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.view

import kalix.scalasdk.Context

/** Context for views. */
trait ViewContext extends Context {

  /**
   * The id of the view that this context is for.
   *
   * @return
   *   The view id.
   */
  def viewId: String
}

/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.view

import kalix.scalasdk.view.View

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ViewUpdateRouter

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ViewRouter[S, V <: View[S]](val view: V) extends ViewUpdateRouter {
  def handleUpdate(commandName: String, state: S, event: Any): View.UpdateEffect[S]
}

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ViewMultiTableRouter extends ViewUpdateRouter {
  def viewRouter(eventName: String): ViewRouter[_, _]
}

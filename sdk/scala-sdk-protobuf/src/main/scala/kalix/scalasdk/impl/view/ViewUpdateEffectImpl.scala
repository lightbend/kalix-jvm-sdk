/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.view

import kalix.javasdk
import kalix.scalasdk.view.View

private[scalasdk] object ViewUpdateEffectImpl {
  sealed trait PrimaryUpdateEffect[S] extends View.UpdateEffect[S] {
    def toJavaSdk: javasdk.impl.view.ViewUpdateEffectImpl.PrimaryUpdateEffect[S]
  }

  final case class Update[S](state: S) extends PrimaryUpdateEffect[S] {
    override def toJavaSdk =
      javasdk.impl.view.ViewUpdateEffectImpl.Update(state)
  }

  case object Delete extends PrimaryUpdateEffect[Nothing] {
    override def toJavaSdk =
      javasdk.impl.view.ViewUpdateEffectImpl.Delete
        .asInstanceOf[javasdk.impl.view.ViewUpdateEffectImpl.PrimaryUpdateEffect[Nothing]]
  }

  case object Ignore extends PrimaryUpdateEffect[Nothing] {
    override def toJavaSdk =
      javasdk.impl.view.ViewUpdateEffectImpl.Ignore
        .asInstanceOf[javasdk.impl.view.ViewUpdateEffectImpl.PrimaryUpdateEffect[Nothing]]
  }

  final case class Error[T](description: String) extends PrimaryUpdateEffect[T] {
    override def toJavaSdk =
      javasdk.impl.view.ViewUpdateEffectImpl.Error(description)
  }
}

/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.valueentity

import kalix.scalasdk.EntityOptions

/** Root entity options for all value based entities. */
trait ValueEntityOptions extends EntityOptions {
  def withForwardHeaders(headers: Set[String]): ValueEntityOptions
}
object ValueEntityOptions {
  val defaults: ValueEntityOptions = ValueEntityOptionsImpl(Set.empty)

  private[kalix] final case class ValueEntityOptionsImpl(forwardHeaders: Set[String]) extends ValueEntityOptions {

    override def withForwardHeaders(headers: Set[String]): ValueEntityOptions =
      copy(forwardHeaders = headers)

  }
}

/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.scalasdk.valueentity

import scala.collection.immutable.Set

import kalix.scalasdk
import kalix.scalasdk.EntityOptions
import kalix.scalasdk.PassivationStrategy

/** Root entity options for all value based entities. */
trait ValueEntityOptions extends EntityOptions {
  def withPassivationStrategy(strategy: PassivationStrategy): ValueEntityOptions
  def withForwardHeaders(headers: Set[String]): ValueEntityOptions
}
object ValueEntityOptions {
  val defaults: ValueEntityOptions =
    ValueEntityOptionsImpl(PassivationStrategy.defaultTimeout, Set.empty)

  private[kalix] final case class ValueEntityOptionsImpl(
      passivationStrategy: scalasdk.PassivationStrategy,
      forwardHeaders: Set[String])
      extends ValueEntityOptions {

    override def withForwardHeaders(headers: Set[String]): ValueEntityOptions =
      copy(forwardHeaders = headers)

    override def withPassivationStrategy(passivationStrategy: scalasdk.PassivationStrategy): ValueEntityOptions =
      copy(passivationStrategy = passivationStrategy)
  }
}

/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.impl.valueentity

import kalix.javasdk.PassivationStrategy
import kalix.javasdk.valueentity.ValueEntityOptions

import java.util.Collections
import java.util

private[impl] case class ValueEntityOptionsImpl(
    override val passivationStrategy: PassivationStrategy,
    override val forwardHeaders: java.util.Set[String])
    extends ValueEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): ValueEntityOptions =
    copy(passivationStrategy = strategy)

  override def withForwardHeaders(headers: util.Set[String]): ValueEntityOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)))
}
object ValueEntityOptionsImpl {
  val defaults = new ValueEntityOptionsImpl(PassivationStrategy.defaultTimeout(), Collections.emptySet())
}

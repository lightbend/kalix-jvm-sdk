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

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.PassivationStrategy
import kalix.javasdk.replicatedentity.{ ReplicatedEntityOptions, WriteConsistency }

import java.util.Collections
import java.util

private[impl] case class ReplicatedEntityOptionsImpl(
    override val passivationStrategy: PassivationStrategy,
    override val forwardHeaders: java.util.Set[String],
    override val writeConsistency: WriteConsistency)
    extends ReplicatedEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): ReplicatedEntityOptions =
    copy(passivationStrategy = strategy)

  override def withWriteConsistency(writeConsistency: WriteConsistency): ReplicatedEntityOptions =
    copy(writeConsistency = writeConsistency)

  override def withForwardHeaders(headers: util.Set[String]): ReplicatedEntityOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)));
}

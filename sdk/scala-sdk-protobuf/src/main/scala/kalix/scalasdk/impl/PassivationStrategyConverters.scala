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

package kalix.scalasdk.impl

import scala.compat.java8.DurationConverters.DurationOps
import scala.compat.java8.DurationConverters.FiniteDurationops

import kalix.scalasdk
import kalix.javasdk

private[scalasdk] object PassivationStrategyConverters {

  def toJava(strategy: scalasdk.PassivationStrategy): javasdk.PassivationStrategy =
    strategy match {
      case scalasdk.impl.Timeout(Some(duration)) => javasdk.PassivationStrategy.timeout(duration.toJava)
      case scalasdk.impl.Timeout(None)           => javasdk.PassivationStrategy.defaultTimeout()
    }

  def toScala(strategy: javasdk.PassivationStrategy): scalasdk.PassivationStrategy =
    strategy match {
      case javasdk.impl.Timeout(Some(duration)) => scalasdk.PassivationStrategy.timeout(duration.toScala)
      case javasdk.impl.Timeout(None)           => scalasdk.PassivationStrategy.defaultTimeout
    }
}

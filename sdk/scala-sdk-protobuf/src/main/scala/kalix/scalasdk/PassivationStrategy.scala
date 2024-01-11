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

package kalix.scalasdk

import scala.concurrent.duration.FiniteDuration

import kalix.scalasdk.impl.Timeout

/** A passivation strategy. */
object PassivationStrategy extends PassivationStrategy

/** A passivation strategy. */
trait PassivationStrategy {

  /**
   * Create a passivation strategy that passivates the entity after the default duration (30 seconds) of inactivity.
   *
   * @return
   *   the passivation strategy
   */
  def defaultTimeout: PassivationStrategy = Timeout(None)

  /**
   * Create a passivation strategy that passivates the entity after a given duration of inactivity.
   *
   * @param duration
   *   of inactivity after which the passivation should occur.
   * @return
   *   the passivation strategy
   */
  def timeout(duration: FiniteDuration): PassivationStrategy = Timeout(Some(duration))
}

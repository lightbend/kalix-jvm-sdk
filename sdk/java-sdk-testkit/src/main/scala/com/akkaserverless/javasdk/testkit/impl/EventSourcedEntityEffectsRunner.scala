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

package com.akkaserverless.javasdk.testkit.impl

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.javasdk.testkit.EventSourcedResult
import com.akkaserverless.javasdk.testkit.impl.EventSourcedResultImpl
import java.util.Optional
import java.util.{ List => JList }
import scala.jdk.CollectionConverters._

/** Extended by generated code, not meant for user extension */
abstract class EventSourcedEntityEffectsRunner[S](entity: EventSourcedEntity[S]) {
  protected var _state: S
  protected var events: JList[Any]
  protected val commandContext: CommandContext

  protected def handleEvent(state: S, event: Any): S

  protected def interpretEffects[R](effect: () => EventSourcedEntity.Effect[S]): EventSourcedResult[R] = {
    val effectExecuted =
      try {
        entity._internalSetCommandContext(Optional.of(commandContext))
        val effectExecuted = effect()
        val events = EventSourcedResultImpl.eventsOf(effectExecuted)
        this.events.addAll(events)
        effectExecuted
      } finally {
        entity._internalSetCommandContext(Optional.empty())
      }

    this._state = events.asScala.foldLeft(this._state)(handleEvent)
    val result =
      try {
        entity._internalSetCommandContext(Optional.of(commandContext))
        val secondaryEffect = EventSourcedResultImpl.secondaryEffectOf(effectExecuted, _state)
        new EventSourcedResultImpl[R, S](effectExecuted, _state, secondaryEffect)
      } finally {
        entity._internalSetCommandContext(Optional.empty())
      }
    result
  }
}

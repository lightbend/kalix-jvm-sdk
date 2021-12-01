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

package com.akkaserverless.scalasdk.testkit.impl

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.akkaserverless.scalasdk.testkit.impl.EventSourcedResultImpl
import scala.collection.immutable.Seq

import com.akkaserverless.scalasdk.eventsourcedentity.CommandContext

abstract class EventSourcedEntityEffectsRunner[S](entity: EventSourcedEntity[S]) {
  var _state: S
  var events: Seq[Any]
  val commandContext: CommandContext

  def handleEvent(state: S, event: Any): S

  def interpretEffects[R](effect: () => EventSourcedEntity.Effect[R]): EventSourcedResult[R] = {
    val effectExecuted =
      try {
        entity._internalSetCommandContext(Some(commandContext))
        val effectExecuted = effect()
        val events = EventSourcedResultImpl.eventsOf(effectExecuted)
        this.events ++= events
        effectExecuted
      } finally {
        entity._internalSetCommandContext(None)
      }

    this._state = events.foldLeft(this._state)(handleEvent)
    val result =
      try {
        entity._internalSetCommandContext(Some(commandContext))
        val secondaryEffect = EventSourcedResultImpl.secondaryEffectOf(effectExecuted, _state)
        new EventSourcedResultImpl[R, S](effectExecuted, _state, secondaryEffect)
      } finally {
        entity._internalSetCommandContext(None)
      }
    result
  }
}

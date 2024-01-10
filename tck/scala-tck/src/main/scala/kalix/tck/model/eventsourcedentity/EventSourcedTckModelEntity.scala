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

package kalix.tck.model.eventsourcedentity

import kalix.scalasdk.SideEffect
import kalix.scalasdk.eventsourcedentity.{ EventSourcedEntity, EventSourcedEntityContext }

class EventSourcedTckModelEntity(context: EventSourcedEntityContext) extends AbstractEventSourcedTckModelEntity {
  override def emptyState: Persisted = Persisted.defaultInstance

  override def process(currentState: Persisted, request: Request): EventSourcedEntity.Effect[Response] = {
    case class HandlingState(
        failed: Boolean,
        events: Seq[Persisted],
        effect: EventSourcedEntity.Effect[Response],
        sideEffects: Seq[SideEffect]) {
      def handle(action: RequestAction): HandlingState = action.action match {
        case RequestAction.Action.Fail(Fail(message, _)) =>
          copy(failed = true, effect = effects.error(message))
        case _ if failed                => this
        case RequestAction.Action.Empty => this
        case RequestAction.Action.Emit(Emit(value, _)) =>
          val newEvents = events :+ Persisted(value)
          copy(
            events = newEvents,
            effect = effects.emitEvents(newEvents.toList).thenReply(state => Response(state.value)))

        case RequestAction.Action.Forward(Forward(id, _)) =>
          val call = components.eventSourcedTwoEntity.call(Request(id))
          copy(effect = effects.emitEvents(events.toList).thenForward(_ => call))

        case RequestAction.Action.Effect(Effect(id, sync, _)) =>
          copy(sideEffects = sideEffects :+ SideEffect(components.eventSourcedTwoEntity.call(Request(id)), sync))
      }

      def result: EventSourcedEntity.Effect[Response] =
        effect.addSideEffects(sideEffects)
    }

    request.actions
      .foldLeft(HandlingState(failed = false, Vector.empty, effects.reply(Response(currentState.value)), Vector.empty))(
        _.handle(_))
      .result
  }

  override def persisted(currentState: Persisted, persisted: Persisted): Persisted =
    Persisted(currentState.value + persisted.value)
}

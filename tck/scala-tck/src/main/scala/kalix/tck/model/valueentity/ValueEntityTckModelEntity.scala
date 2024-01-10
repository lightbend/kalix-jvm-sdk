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

package kalix.tck.model.valueentity

import kalix.scalasdk.SideEffect
import kalix.scalasdk.valueentity.{ ValueEntity, ValueEntityContext }

/** A value entity. */
class ValueEntityTckModelEntity(context: ValueEntityContext) extends AbstractValueEntityTckModelEntity {
  override def emptyState: Persisted = Persisted.defaultInstance

  override def process(currentState: Persisted, request: Request): ValueEntity.Effect[Response] = {
    case class HandlingState(
        state: String,
        effect: Either[ValueEntity.Effect.OnSuccessBuilder[Persisted], ValueEntity.Effect[Response]],
        sideEffect: Seq[SideEffect]) {
      def handle(action: RequestAction): HandlingState =
        action.action match {
          case RequestAction.Action.Empty => this
          case RequestAction.Action.Update(Update(newValue, _)) =>
            copy(state = newValue, effect = Left(effects.updateState(Persisted(newValue))))
          case RequestAction.Action.Delete(Delete(_)) =>
            copy(state = "", effect = Left(effects.deleteEntity()))
          case RequestAction.Action.Fail(Fail(message, _)) =>
            copy(effect = Right(effects.error(message)))
          case RequestAction.Action.Effect(Effect(id, sync, _)) =>
            copy(sideEffect = sideEffect :+ SideEffect(components.valueEntityTwoEntity.call(Request(id)), sync))
          case RequestAction.Action.Forward(Forward(id, _)) =>
            val call = components.valueEntityTwoEntity.call(Request(id))
            val newEffect: Either[ValueEntity.Effect.OnSuccessBuilder[Persisted], ValueEntity.Effect[Response]] =
              effect match {
                case Left(e)  => Right(e.thenForward(call))
                case Right(_) => Right(effects.forward(call))

              }
            copy(effect = newEffect)
        }
      def result: ValueEntity.Effect[Response] =
        (effect match {
          case Left(e)  => e.thenReply(Response(state))
          case Right(e) => e
        }).addSideEffects(sideEffect)
    }

    request.actions
      .foldLeft(HandlingState(currentState.value, Right(effects.reply(Response(currentState.value))), Vector.empty))(
        _.handle(_))
      .result
  }
}

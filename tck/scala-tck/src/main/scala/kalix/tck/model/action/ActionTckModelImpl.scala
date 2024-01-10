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

package kalix.tck.model.action

import akka.NotUsed
import akka.stream.scaladsl.{ Sink, Source }
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

class ActionTckModelImpl(ctx: ActionCreationContext) extends AbstractActionTckModelAction {
  private implicit val mat = ctx.materializer()

  override def processUnary(request: Request): Action.Effect[Response] =
    response(request.groups)

  override def processStreamedIn(requestSrc: Source[Request, NotUsed]): Action.Effect[Response] =
    effects.asyncEffect(
      requestSrc
        // collect all requests
        .runWith(Sink.seq)
        // and then create a single resource
        .map(reqs => response(reqs.flatMap(_.groups))))

  override def processStreamedOut(request: Request): Source[Action.Effect[Response], NotUsed] =
    // each group can create a response
    Source(request.groups).map(g => response(g :: Nil))

  override def processStreamed(requestSrc: Source[Request, NotUsed]): Source[Action.Effect[Response], NotUsed] =
    requestSrc.mapConcat(_.groups).map(g => response(g :: Nil))

  def response(groups: Seq[ProcessGroup]): Action.Effect[Response] = {
    val allSteps = groups.flatMap(_.steps).map(_.step)
    val failed = allSteps.reverse.collectFirst { case ProcessStep.Step.Fail(f) =>
      effects.error(f.message)
    }
    val sideEffects =
      allSteps.collect { case ProcessStep.Step.Effect(e) =>
        kalix.scalasdk.SideEffect(components.actionTwoImpl.call(OtherRequest(e.id)), e.synchronous)
      }

    val res =
      failed match {
        case Some(e) => e
        case None =>
          allSteps.reverse
            .collectFirst {
              case ProcessStep.Step.Reply(r)   => effects.reply(Response(r.message))
              case ProcessStep.Step.Forward(f) => effects.forward(components.actionTwoImpl.call(OtherRequest(f.id)))
            }
            .getOrElse(effects.reply(Response.defaultInstance))
      }
    res.addSideEffects(sideEffects)
  }
}

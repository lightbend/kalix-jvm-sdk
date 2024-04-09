/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.action

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

class ActionTckModelImpl(ctx: ActionCreationContext) extends AbstractActionTckModelAction {
  private implicit val mat: Materializer = ctx.materializer()

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

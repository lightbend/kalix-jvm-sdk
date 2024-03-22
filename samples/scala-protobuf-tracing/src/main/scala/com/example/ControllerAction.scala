package com.example

import com.google.protobuf.empty.Empty
import io.opentelemetry.api.trace.{StatusCode, Tracer}
import io.opentelemetry.context.Scope
import kalix.scalasdk.action.{Action, ActionCreationContext}
import sttp.client4.quick.RichRequest
import sttp.client4.{Response, _}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ControllerAction(creationContext: ActionCreationContext) extends AbstractControllerAction {

  val url = "https://jsonplaceholder.typicode.com/posts/1"

  override def callAsyncEndpoint(empty: Empty): Action.Effect[MessageResponse] = {
    val tracerOpt = actionContext.getOpenTelemetryTracer
    tracerOpt match {
      case Some(tracer) =>
        val responseBody = callAsync(tracer)
        effects.asyncReply(responseBody)
      case None =>
        val responseBody = callAsync()
        effects.asyncReply(responseBody)
    }
  }

  private def callAsync(tracer: Tracer): Future[MessageResponse] = {
    val span = tracer
      .spanBuilder("loreipsumendpoint")
      .setParent(actionContext.metadata.traceContext.asOpenTelemetryContext)
      .startSpan()
    val scope: Scope = span.makeCurrent()
    try {
      val responseBody: Future[MessageResponse] = callAsync()
      responseBody.onComplete {
        case Success(response) => span.setAttribute("result", response.message)
        case Failure(exception) => span.setStatus(StatusCode.ERROR, exception.getMessage)
      }
      responseBody
    } finally {
      span.end()
      scope.close()
    }
  }

  private def callAsync(): Future[MessageResponse] = {
    val request = basicRequest.get(uri"$url")
    val response: Future[Response[Either[String, String]]] =
      request.send(Main.backend)
    response.map { response =>
      response.body match {
        case Left(resEx) =>
          MessageResponse(resEx)
        case Right(value) =>
          MessageResponse(value)
      }
    }
  }

}
